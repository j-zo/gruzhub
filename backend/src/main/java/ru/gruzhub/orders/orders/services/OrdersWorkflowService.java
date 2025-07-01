package ru.gruzhub.orders.orders.services;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gruzhub.orders.orders.commands.CreateOrderCommand;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.models.OrderStatusChange;
import ru.gruzhub.orders.orders.repositories.OrderRepository;
import ru.gruzhub.orders.orders.repositories.OrderStatusChangeRepository;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.telegram.services.TelegramSenderService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrdersWorkflowService {
    private static final BigDecimal TAKE_ORDER_PRICE_RUB = new BigDecimal("2000");

    private final OrderRepository orderRepository;
    private final OrderStatusChangeRepository orderStatusChangeRepository;
    private final UsersService usersService;
    private final CreateOrderCommand createOrderCommand;
    private final TelegramSenderService telegramSenderService;

    @Value("${app.url}")
    private String siteMainUrl;

    public CreateOrderResponseDto createOrder(CreateOrderRequestDto order) {
        return this.createOrderCommand.createOrder(order);
    }

    public void startCalculationByMaster(User authorizedUser, Long orderId) {
        if (authorizedUser.getRole() != UserRole.MASTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Order can be taken into work only by MASTER");
        }

        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if (authorizedUser.getAddress() == null ||
                !authorizedUser.getAddress()
                        .getRegion()
                        .getId()
                        .equals(order.getAddress().getRegion().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access to foreign region order");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Заказ уже взят в работу другим автосервисом");
        }

        if (authorizedUser.getBalance().compareTo(TAKE_ORDER_PRICE_RUB) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "На балансе недостаточно средств, чтобы взять заказ" +
                            " в работу");
        }

        if (order.getDeclinedMastersIds().contains(authorizedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Текущий автосервис не может взять этот заказ");
        }

        order.setStatus(OrderStatus.CALCULATING);
        order.setLastStatusUpdateTime(System.currentTimeMillis());
        order.setMaster(authorizedUser);
        this.usersService.decreaseUserBalance(authorizedUser.getId(), TAKE_ORDER_PRICE_RUB);
        order = this.orderRepository.save(order);

        OrderStatusChange orderStatusChange = new OrderStatusChange();
        orderStatusChange.setOrder(order);
        orderStatusChange.setNewStatus(OrderStatus.CALCULATING);
        orderStatusChange.setUpdatedAt(System.currentTimeMillis());
        orderStatusChange.setUpdatedBy(authorizedUser);
        this.orderStatusChangeRepository.save(orderStatusChange);
    }

    public void declineOrderMaster(User user, Long orderId, String comment) {
        this.removeMasterFromOrderAndRefundMaster(user, orderId, OrderStatus.CREATED, comment);
    }

    public void sendForConfirmationByMaster(User user, Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if (order.getMaster() == null ||
                !Objects.equals(user.getId(), order.getMaster().getId()) ||
                order.getStatus() != OrderStatus.CALCULATING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        order.setStatus(OrderStatus.REVIEWING);
        order.setLastStatusUpdateTime(System.currentTimeMillis());

        OrderStatusChange orderStatusChange = new OrderStatusChange();
        orderStatusChange.setOrder(order);
        orderStatusChange.setNewStatus(OrderStatus.REVIEWING);
        orderStatusChange.setUpdatedAt(System.currentTimeMillis());
        orderStatusChange.setUpdatedBy(user);
        orderStatusChange.setMaster(order.getMaster());

        this.orderRepository.save(order);
        this.orderStatusChangeRepository.save(orderStatusChange);

        this.sendTelegramMessage(orderId,
                user,
                "Заказ #" +
                        orderId +
                        " отправлен на согласование. Подтвердите, что можно " +
                        "начинать работу");
    }

    public void acceptByCustomer(User user, Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if ((order.getCustomer() == null || !user.getId().equals(order.getCustomer().getId())) ||
                order.getStatus() != OrderStatus.REVIEWING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (order.getMaster() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Order does not have master");
        }

        order.setStatus(OrderStatus.ACCEPTED);
        order.setLastStatusUpdateTime(System.currentTimeMillis());

        OrderStatusChange orderStatusChange = new OrderStatusChange();
        orderStatusChange.setOrder(order);
        orderStatusChange.setNewStatus(OrderStatus.ACCEPTED);
        orderStatusChange.setUpdatedAt(System.currentTimeMillis());
        orderStatusChange.setUpdatedBy(user);
        orderStatusChange.setMaster(order.getMaster());

        this.orderRepository.save(order);
        this.orderStatusChangeRepository.save(orderStatusChange);

        this.sendTelegramMessage(orderId,
                order.getMaster(),
                "Заказ #" +
                        orderId +
                        " согласован заказчиком. Можете приступать к работе");
    }

    public void completeOrder(User authorizedUser, Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if ((order.getCustomer() == null ||
                !authorizedUser.getId().equals(order.getCustomer().getId())) &&
                (order.getDriver() == null ||
                        !authorizedUser.getId().equals(order.getDriver().getId())) &&
                (order.getMaster() == null ||
                        !authorizedUser.getId().equals(order.getMaster().getId())) &&
                authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setLastStatusUpdateTime(System.currentTimeMillis());

        OrderStatusChange orderStatusChange = new OrderStatusChange();
        orderStatusChange.setOrder(order);
        orderStatusChange.setNewStatus(OrderStatus.COMPLETED);
        orderStatusChange.setUpdatedAt(System.currentTimeMillis());
        orderStatusChange.setUpdatedBy(authorizedUser);
        orderStatusChange.setMaster(order.getMaster());

        this.orderRepository.save(order);
        this.orderStatusChangeRepository.save(orderStatusChange);

        if (authorizedUser.getRole() == UserRole.MASTER) {
            this.sendTelegramMessage(orderId,
                    order.getCustomer(),
                    "Заказ #" + orderId + " завершён");
        }

        if ((authorizedUser.getRole() == UserRole.CUSTOMER ||
                authorizedUser.getRole() == UserRole.DRIVER) && order.getMaster() != null) {
            this.sendTelegramMessage(orderId, order.getMaster(), "Заказ #" + orderId + " завершён");
        }
    }

    public void cancelOrder(User user, Long orderId, String comment) {
        if (user.getRole() == UserRole.MASTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        this.removeMasterFromOrderAndRefundMaster(user, orderId, OrderStatus.CANCEL, comment);
    }

    public void updateOrderStatusForTesting(Long orderId, OrderStatus status) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        this.orderRepository.save(order);
    }

    private void sendTelegramMessage(Long orderId, User user, String message) {
        Thread.startVirtualThread(() -> {
            if (user.getConnectedTelegramChats().isEmpty()) {
                return;
            }

            try {
                SignInUserResponseDto authDto = this.usersService.getUserAccessNoAuth(user.getId());
                String authLink = siteMainUrl + "?userId=" +
                        authDto.getId() +
                        "&accessToken=" +
                        authDto.getAccessToken() +
                        "&orderId=" +
                        orderId;

                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("Посмотреть");
                button.setUrl(authLink);
                keyboardMarkup.setKeyboard(List.of(List.of(button)));

                for (TelegramChat chat : user.getConnectedTelegramChats()) {
                    this.telegramSenderService.sendMessage(chat.getTelegramChatId(),
                            message,
                            keyboardMarkup);
                }
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        });
    }

    private void removeMasterFromOrderAndRefundMaster(User authorizedUser,
                                                      Long orderId,
                                                      OrderStatus newStatus,
                                                      String comment) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();
        User orderMaster = order.getMaster();

        if (newStatus != OrderStatus.CREATED && newStatus != OrderStatus.CANCEL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверный статус заказа");
        }

        if ((order.getCustomer() != null &&
                !authorizedUser.getId().equals(order.getCustomer().getId())) &&
                (order.getMaster() != null &&
                        !authorizedUser.getId().equals(order.getMaster().getId())) &&
                authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // Customer decided to change master
        if (newStatus == OrderStatus.CREATED) {
            if (order.getMaster() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "У заказа нет исполнителя");
            }

            order.addDeclinedMaster(order.getMaster().getId());
        }

        order.setMaster(null);
        order.setStatus(newStatus);
        order.setLastStatusUpdateTime(System.currentTimeMillis());

        if (orderMaster != null) {
            this.usersService.increaseUserBalance(orderMaster.getId(), TAKE_ORDER_PRICE_RUB);
        }

        OrderStatusChange orderStatusChange = new OrderStatusChange();
        orderStatusChange.setOrder(order);
        orderStatusChange.setNewStatus(newStatus);
        orderStatusChange.setUpdatedAt(System.currentTimeMillis());
        orderStatusChange.setUpdatedBy(authorizedUser);
        orderStatusChange.setComment(comment);

        this.orderRepository.save(order);
        this.orderStatusChangeRepository.save(orderStatusChange);
    }
}
