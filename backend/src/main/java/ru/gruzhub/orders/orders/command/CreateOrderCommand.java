package ru.gruzhub.orders.orders.command;

import io.sentry.Sentry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gruzhub.address.service.AddressesService;
import ru.gruzhub.address.service.RegionsService;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.address.models.Region;
import ru.gruzhub.driver.DriverRepository;
import ru.gruzhub.driver.model.Driver;
import ru.gruzhub.transport.TransportService;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.model.OrderStatusChange;
import ru.gruzhub.orders.orders.repository.OrderRepository;
import ru.gruzhub.orders.orders.repository.OrderStatusChangeRepository;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.telegram.services.TelegramSenderService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateOrderCommand {
    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final OrderStatusChangeRepository orderStatusChangeRepository;
    private final UsersService usersService;
    private final AddressesService addressesService;
    private final TransportService transportService;
    private final RegionsService regionsService;
    private final TelegramSenderService telegramSenderService;

    @Value("${app.url}")
    private String appUrl;

    @Transactional
    public CreateOrderResponseDto createOrder(CreateOrderRequestDto createOrderRequest) {
        User authorizedUser = this.usersService.getCurrentUser();

        CreateOrderResponseDto createOrderResponseDto = this.getResponseIfDuplicatedOrder(createOrderRequest);
        if (createOrderResponseDto != null) {
            return createOrderResponseDto;
        }

        Driver driver = this.getOrCreateAndValidateDriverIfPresentInOrder(createOrderRequest);
        User customer = this.getCustomerIfAuthorized(authorizedUser);
        Address address = this.createAddress(createOrderRequest);
        List<Transport> transport = this.getOrCreateTransport(createOrderRequest);

        Order order = Order.builder()
                .guaranteeUuid(createOrderRequest.getGuaranteeUuid())
                .customer(customer)
                .driver(driver)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .address(address)
                .transport(transport)
                .status(OrderStatus.CREATED)
                .lastStatusUpdateTime(System.currentTimeMillis())
                .description(createOrderRequest.getDescription())
                .notes(createOrderRequest.getNotes())
                .isNeedEvacuator(createOrderRequest.isNeedEvacuator())
                .isNeedMobileTeam(createOrderRequest.isNeedMobileTeam())
                .urgency(createOrderRequest.getUrgency())
                .build();
        order = this.orderRepository.save(order);

        OrderStatusChange orderStatusChange = OrderStatusChange.builder()
                .order(order)
                .newStatus(OrderStatus.CREATED)
                .updatedAt(System.currentTimeMillis())
                .updatedBy(authorizedUser)
                .build();
        this.orderStatusChangeRepository.save(orderStatusChange);

        Order finalOrder = order;
        Thread.startVirtualThread(() -> this.sendNotificationToRegionMasters(finalOrder.getId()));

        return new CreateOrderResponseDto(finalOrder.getId(), driver.getId());
    }

    private CreateOrderResponseDto getResponseIfDuplicatedOrder(CreateOrderRequestDto requestDto) {
        Order duplicateOrder =
                this.orderRepository.findOrderByGuaranteeUuid(requestDto.getGuaranteeUuid());

        if (duplicateOrder != null && duplicateOrder.getDriver() != null) {
            return new CreateOrderResponseDto(duplicateOrder.getId(), duplicateOrder.getDriver().getId());
        }

        return null;
    }

    private Driver getOrCreateAndValidateDriverIfPresentInOrder(CreateOrderRequestDto createOrderRequest) {
        // case if existing driver
        Optional<Driver> driver = driverRepository.findByNameOrEmailOrPhone(
                createOrderRequest.getDriverName(),
                createOrderRequest.getDriverEmail(),
                createOrderRequest.getDriverPhone());

        if (driver.isPresent()) {
            return driver.get();
        }

        // case if new driver
        if (!StringUtils.isBlank(createOrderRequest.getDriverPhone()) ||
                !StringUtils.isBlank(createOrderRequest.getDriverEmail()) ||
                !StringUtils.isBlank(createOrderRequest.getDriverName())) {

           Driver newDriver = Driver.builder()
                   .phone(createOrderRequest.getDriverPhone())
                   .email(createOrderRequest.getDriverEmail())
                   .name(createOrderRequest.getDriverName())
                   .build();

            return this.driverRepository.save(newDriver);
        }

        throw new RuntimeException("Driver should be present in the order!");
    }

    private User getCustomerIfAuthorized(User authorizedUser) {
        return authorizedUser != null && authorizedUser.getRole() == UserRole.CUSTOMER ?
                authorizedUser :
                null;
    }

    private Address createAddress(CreateOrderRequestDto createOrderRequest) {
        Region region = this.regionsService.getRegionById(createOrderRequest.getRegionId());
        Address address = new Address(null,
                region,
                createOrderRequest.getCity(),
                createOrderRequest.getStreet(),
                null,
                null);
        return this.addressesService.createAddress(address);
    }

    private List<Transport> getOrCreateTransport(CreateOrderRequestDto createOrderRequest) {

        return createOrderRequest.getTransport().stream().map(transportDto -> {
            Transport transport;

            if (transportDto.getId() != null) {
                transport = this.transportService.getTransportById(transportDto.getId());
            } else {
                transport = this.transportService.createTransport(transportDto);
            }

            return transport;
        }).toList();
    }

    private void sendNotificationToRegionMasters(Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();
        List<User> mastersInRegion =
                this.usersService.getMastersWithTelegramInRegion(order.getAddress()
                        .getRegion()
                        .getId());
        List<User> admins = this.usersService.getAdmins();

        List<User> mastersAndAdmins = new ArrayList<>();
        mastersAndAdmins.addAll(mastersInRegion);
        mastersAndAdmins.addAll(admins);

        String message = this.getCreatedOrderMessage(order);

        for (User masterOrAdmin : mastersAndAdmins) {
            if (masterOrAdmin.getConnectedTelegramChats().isEmpty()) {
                continue;
            }

            SignInUserResponseDto masterAuth =
                    this.usersService.getUserAccessNoAuth(masterOrAdmin.getId());
            String authLink = appUrl + "?userId=" +
                    masterAuth.getId() +
                    "&accessToken=" +
                    masterAuth.getAccessToken() +
                    "&orderId=" +
                    order.getId();

            try {
                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                keyboardMarkup.setKeyboard(rows);

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("Посмотреть");
                button.setUrl(authLink);
                rows.add(List.of(button));

                if (masterOrAdmin.getConnectedTelegramChats() != null) {
                    for (TelegramChat chat : masterOrAdmin.getConnectedTelegramChats()) {
                        this.telegramSenderService.sendMessage(chat.getTelegramChatId(),
                                message,
                                keyboardMarkup);
                    }
                }
            } catch (Exception e) {
                Sentry.captureException(e);
            }
        }
    }

    private String getCreatedOrderMessage(Order order) {
        StringBuilder message = new StringBuilder("Создан заказ #").append(order.getId())
                .append(" в регионе ")
                .append(order.getAddress()
                        .getRegion()
                        .getName());

        for (Transport transport : order.getTransport()) {
            if (transport.getType() == TransportType.TRUCK) {
                message.append("\n\nГрузовик").append("\nМарка: ").append(transport.getBrand());
                if (transport.getModel() != null) {
                    message.append("\nМодель: ").append(transport.getModel());
                }
            }
            if (transport.getType() == TransportType.TRAILER && transport.getModel() != null) {
                message.append("\n\nПрицеп").append("\nТип: ").append(transport.getModel());
            }
        }

        message.append("\n\n").append(order.getDescription());

        if (order.isNeedEvacuator() || order.isNeedMobileTeam()) {
            message.append("\n");
            if (order.isNeedEvacuator()) {
                message.append("\n- Требуется эвакуатор");
            }
            if (order.isNeedMobileTeam()) {
                message.append("\n- Требуется выездная бригада");
            }
        }

        message.append("\n\nСрочность: ")
                .append(order.getUrgency())
                .append("\n\nДля просмотра - " + appUrl);

        return message.toString();
    }
}
