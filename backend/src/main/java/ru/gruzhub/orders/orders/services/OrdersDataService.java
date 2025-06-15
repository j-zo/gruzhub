package ru.gruzhub.orders.orders.services;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.orders.auto.AutoService;
import ru.gruzhub.orders.auto.dto.AutoResponseDto;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.orders.orders.commands.UpdateAutoCommand;
import ru.gruzhub.orders.orders.dto.GetOrdersRequestDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.dto.UpdateOrderAutoRequestDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.models.OrderStatusChange;
import ru.gruzhub.orders.orders.repositories.OrderQueryRepository;
import ru.gruzhub.orders.orders.repositories.OrderRepository;
import ru.gruzhub.orders.orders.repositories.OrderStatusChangeRepository;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.models.UserInfoChange;

@Service
@RequiredArgsConstructor
public class OrdersDataService {
    private final OrderRepository orderRepository;
    private final OrderStatusChangeRepository orderStatusChangeRepository;
    private final UsersService usersService;
    private final AutoService autoService;
    private final OrderQueryRepository orderQueryRepository;

    public Order getOrderById(User authorizedUser, Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if (authorizedUser.getRole() == UserRole.MASTER) {
            if (authorizedUser.getAddress() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                  "У мастера нет адреса");
            }

            if (order.getDeclinedMastersIds().contains(authorizedUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                  "К сожалению, у вас нет доступа к этому заказу");
            }

            boolean isOrderAndMasterSameRegion =
                Objects.equals(order.getAddress().getRegion().getId(),
                               authorizedUser.getAddress().getRegion().getId());

            if (!isOrderAndMasterSameRegion) {
                if (order.getMaster() == null ||
                    !Objects.equals(order.getMaster().getId(), authorizedUser.getId())) {

                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                      "Заказ в другом регионе");
                }
            }

            User orderMaster = order.getMaster();
            if (orderMaster != null &&
                !Objects.equals(orderMaster.getId(), authorizedUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                  "Заказ прикреплен к другому СТО");
            }
        } else if ((order.getDriver() != null &&
                    !authorizedUser.getId().equals(order.getDriver().getId())) &&
                   (order.getCustomer() != null &&
                    !authorizedUser.getId().equals(order.getCustomer().getId())) &&
                   authorizedUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return order;
    }

    public List<OrderResponseDto> getAutoOrders(User user, Long autoId) {
        if (user.getRole() == UserRole.ADMIN) {
            return this.orderRepository.findOrdersByAuto(autoId)
                                       .stream()
                                       .map(OrderResponseDto::new)
                                       .toList();
        }

        Auto auto = this.autoService.getAutoById(autoId);

        if (!Objects.equals(auto.getDriver().getId(), user.getId()) &&
            !Objects.equals(auto.getCustomer().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return this.orderRepository.findOrdersByAuto(autoId)
                                   .stream()
                                   .map(OrderResponseDto::new)
                                   .toList();
    }

    public List<Order> getOrders(User user, GetOrdersRequestDto requestDto) {
        return switch (user.getRole()) {
            case UserRole.DRIVER -> this.orderQueryRepository.findOrders(null,
                                                                         null,
                                                                         user.getId(),
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         requestDto.getStatuses(),
                                                                         requestDto.getLimit());

            case UserRole.MASTER -> this.orderQueryRepository.findMasterOrders(user.getId(),
                                                                               user.getAddress()
                                                                                   .getRegion()
                                                                                   .getId(),
                                                                               requestDto.getStatuses(),
                                                                               requestDto.getLimit());

            case UserRole.CUSTOMER -> this.orderQueryRepository.findOrders(null,
                                                                           user.getId(),
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           requestDto.getStatuses(),
                                                                           requestDto.getLimit());

            case UserRole.ADMIN -> this.orderQueryRepository.findOrders(requestDto.getMasterId(),
                                                                        requestDto.getCustomerId(),
                                                                        requestDto.getDriverId(),
                                                                        requestDto.getAutoId(),
                                                                        null,
                                                                        requestDto.getRegionsIds(),
                                                                        requestDto.getStatuses(),
                                                                        requestDto.getLimit());
        };
    }

    public AutoResponseDto getOrderAuto(User user, Long orderId, Long autoId) {
        Auto auto = this.autoService.getAutoById(autoId);
        this.validateOrderAutoPermissions(user, orderId, auto);
        return new AutoResponseDto(auto);
    }

    public void updateOrderAuto(User user, UpdateOrderAutoRequestDto requestDto) {
        Auto auto = this.autoService.getAutoById(requestDto.getAutoId());
        this.validateOrderAutoPermissions(user, requestDto.getOrderId(), auto);

        auto.setBrand(requestDto.getBrand());
        auto.setModel(requestDto.getModel());
        auto.setVin(requestDto.getVin());
        auto.setNumber(requestDto.getNumber());

        new UpdateAutoCommand(this.autoService, this.orderRepository).updateAuto(auto);
    }

    public List<UserInfoChange> getUserInfoChanges(User user, Long orderId, Long userId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();

        boolean isAuthorizedUserInOrder =
            (order.getDriver() != null && order.getDriver().getId().equals(user.getId())) ||
            (order.getMaster() != null && order.getMaster().getId().equals(user.getId())) ||
            (order.getCustomer() != null && order.getCustomer().getId().equals(user.getId()));

        boolean isRequestUserInOrder =
            (order.getDriver() != null && order.getDriver().getId().equals(userId)) ||
            (order.getMaster() != null && order.getMaster().getId().equals(userId)) ||
            (order.getCustomer() != null && order.getCustomer().getId().equals(userId));

        if ((isAuthorizedUserInOrder && isRequestUserInOrder) || user.getRole() == UserRole.ADMIN) {
            return this.usersService.getUserInfoChanges(userId);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public List<OrderStatusChange> getOrderStatusChanges(User authorizedUser, Long orderId) {
        Order order = this.orderRepository.findById(orderId).orElseThrow();

        boolean isMasterAccessToCreatedOrder =
            authorizedUser.getRole() == UserRole.MASTER && order.getStatus() == OrderStatus.CREATED;

        boolean isAccessToExistingOrder = (order.getDriver() != null &&
                                           authorizedUser.getId()
                                                         .equals(order.getDriver().getId())) ||
                                          (order.getMaster() != null &&
                                           authorizedUser.getId()
                                                         .equals(order.getMaster().getId())) ||
                                          (order.getCustomer() != null &&
                                           authorizedUser.getId()
                                                         .equals(order.getCustomer().getId())) ||
                                          authorizedUser.getRole() == UserRole.ADMIN;

        if (!isMasterAccessToCreatedOrder && !isAccessToExistingOrder) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return this.orderStatusChangeRepository.findStatusChangesByOrder(order);
    }

    private void validateOrderAutoPermissions(User user, Long orderId, Auto auto) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if (user.getRole() == UserRole.MASTER &&
            (order.getMaster() == null || !order.getMaster().getId().equals(user.getId())) &&
            order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<Long> autosIds = order.getAutos().stream().map(Auto::getId).toList();
        if (!autosIds.contains(auto.getId()) &&
            !Objects.equals(auto.getCustomer().getId(), user.getId()) &&
            !Objects.equals(auto.getDriver().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}