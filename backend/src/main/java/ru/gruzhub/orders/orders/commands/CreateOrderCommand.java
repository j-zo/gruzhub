package ru.gruzhub.orders.orders.commands;

import io.sentry.Sentry;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.gruzhub.address.AddressesService;
import ru.gruzhub.address.RegionsService;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.address.models.Region;
import ru.gruzhub.orders.auto.AutoService;
import ru.gruzhub.orders.auto.enums.AutoType;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.dto.OrderAutoDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.models.OrderStatusChange;
import ru.gruzhub.orders.orders.repositories.OrderRepository;
import ru.gruzhub.orders.orders.repositories.OrderStatusChangeRepository;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.telegram.services.TelegramSenderService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.dto.UpdateUserRequestDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CreateOrderCommand {
    private final OrderRepository orderRepository;
    private final OrderStatusChangeRepository orderStatusChangeRepository;
    private final UsersService usersService;
    private final AddressesService addressesService;
    private final AutoService autoService;
    private final RegionsService regionsService;
    private final TelegramSenderService telegramSenderService;

    @Value("${app.url}")
    private String appUrl;

    public CreateOrderResponseDto createOrder(@Nullable String authorization,
                                              CreateOrderRequestDto createOrderRequest) {
        User authorizedUser = null;
        if (authorization != null) {
            authorizedUser = this.usersService.getUserFromToken(authorization);
        }

        CreateOrderResponseDto createOrderResponseDto =
                this.getResponseIfDuplicatedOrder(createOrderRequest);
        if (createOrderResponseDto != null) {
            return createOrderResponseDto;
        }

        User driver =
                this.getOrCreateAndValidateDriverIfPresentInOrder(createOrderRequest, authorizedUser);
        User customer = this.getCustomerIfAuthorized(authorizedUser);
        Address address = this.createAddress(createOrderRequest);
        List<Auto> autos = this.getOrCreateAutos(createOrderRequest, driver, customer);

        Order order = Order.builder()
                .guaranteeUuid(createOrderRequest.getGuaranteeUuid())
                .customer(customer)
                .driver(driver)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .address(address)
                .autos(autos)
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
                .updatedBy(authorizedUser != null ?
                        authorizedUser :
                        driver)
                .build();
        this.orderStatusChangeRepository.save(orderStatusChange);

        Order finalOrder = order;
        Thread.startVirtualThread(() -> this.sendNotificationToRegionMasters(finalOrder.getId()));

        if (driver != null && authorizedUser == null) {
            return new CreateOrderResponseDto(finalOrder.getId(),
                    driver.getId(),
                    this.usersService.generateAccessToken(driver));
        }

        return new CreateOrderResponseDto(finalOrder.getId(), null, null);
    }

    private CreateOrderResponseDto getResponseIfDuplicatedOrder(CreateOrderRequestDto requestDto) {
        Order duplicateOrder =
                this.orderRepository.findOrderByGuaranteeUuid(requestDto.getGuaranteeUuid());

        if (duplicateOrder != null && duplicateOrder.getDriver() != null) {
            return new CreateOrderResponseDto(duplicateOrder.getId(),
                    duplicateOrder.getDriver().getId(),
                    this.usersService.generateAccessToken(duplicateOrder.getDriver()));
        }

        return null;
    }

    private User getOrCreateAndValidateDriverIfPresentInOrder(CreateOrderRequestDto createOrderRequest,
                                                              @Nullable User authorizedUser) {
        // case if driver creates second order
        if (authorizedUser != null && authorizedUser.getRole() == UserRole.DRIVER) {
            boolean isAnotherDriverData =
                    !Objects.equals(authorizedUser.getPhone(), createOrderRequest.getDriverPhone()) ||
                            !Objects.equals(authorizedUser.getName(), createOrderRequest.getDriverName());

            if (isAnotherDriverData) {
                authorizedUser.setPhone(createOrderRequest.getDriverPhone());
                if (createOrderRequest.getDriverName() != null) {
                    authorizedUser.setName(createOrderRequest.getDriverName());
                }

                UpdateUserRequestDto updateUserRequest =
                        new UpdateUserRequestDto(authorizedUser.getId(),
                                authorizedUser.getName(),
                                authorizedUser.getInn(),
                                authorizedUser.getEmail(),
                                authorizedUser.getPhone(),
                                null,
                                null,
                                createOrderRequest.getRegionId(),
                                createOrderRequest.getCity(),
                                createOrderRequest.getStreet());
                this.usersService.update(authorizedUser, updateUserRequest);
                return authorizedUser;
            }

            return authorizedUser;
        }

        // case if anonymous driver
        if (createOrderRequest.getDriverPhone() != null &&
                createOrderRequest.getDriverName() != null) {
            User driverByPhone =
                    this.usersService.getUserByPhone(createOrderRequest.getDriverPhone(),
                            UserRole.DRIVER);
            if (driverByPhone != null) {
                return driverByPhone;
            }

            return this.usersService.createUser(createOrderRequest.getDriverName(),
                    createOrderRequest.getDriverPhone(),
                    createOrderRequest.getDriverEmail(),
                    UserRole.DRIVER);
        }

        throw new RuntimeException("Driver should be present in any order");
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

    private List<Auto> getOrCreateAutos(CreateOrderRequestDto createOrderRequest,
                                        User driver,
                                        User customer) {
        List<Auto> autos = new ArrayList<>();

        for (OrderAutoDto autoDto : createOrderRequest.getAutos()) {
            Auto auto;

            if (autoDto.getAutoId() != null) {
                auto = this.autoService.getAutoById(autoDto.getAutoId());
            } else {
                auto = Auto.builder()
                        .brand(autoDto.getBrand())
                        .model(autoDto.getModel())
                        .vin(autoDto.getVin())
                        .number(autoDto.getNumber())
                        .type(autoDto.getType())
                        .driver(driver)
                        .customer(customer)
                        .build();
                auto = this.autoService.createAuto(auto);
            }

            autos.add(auto);
        }

        return autos;
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

        for (Auto auto : order.getAutos()) {
            if (auto.getType() == AutoType.TRUCK) {
                message.append("\n\nГрузовик").append("\nМарка: ").append(auto.getBrand());
                if (auto.getModel() != null) {
                    message.append("\nМодель: ").append(auto.getModel());
                }
            }
            if (auto.getType() == AutoType.TRAILER && auto.getModel() != null) {
                message.append("\n\nПрицеп").append("\nТип: ").append(auto.getModel());
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
