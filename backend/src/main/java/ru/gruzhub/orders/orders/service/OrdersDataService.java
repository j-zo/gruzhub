package ru.gruzhub.orders.orders.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.transport.TransportService;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.orders.orders.command.UpdateTransportCommand;
import ru.gruzhub.orders.orders.dto.GetOrdersRequestDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.dto.UpdateOrderTransportRequestDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.model.OrderStatusChange;
import ru.gruzhub.orders.orders.repository.OrderQueryRepository;
import ru.gruzhub.orders.orders.repository.OrderRepository;
import ru.gruzhub.orders.orders.repository.OrderStatusChangeRepository;
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
    private final TransportService transportService;
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

    public List<OrderResponseDto> getTransportOrders(User user, UUID transportId) {
        if (user.getRole() == UserRole.ADMIN) {
            return this.orderRepository.findOrdersByTransport(transportId)
                                       .stream()
                                       .map(OrderResponseDto::new)
                                       .toList();
        }

        Transport transport = this.transportService.getTransportById(transportId);

        if (!Objects.equals(transport.getDriver().getId(), user.getId()) &&
            !Objects.equals(transport.getCustomer().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return this.orderRepository.findOrdersByTransport(transportId)
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
                                                                        requestDto.getTransportId(),
                                                                        null,
                                                                        requestDto.getRegionsIds(),
                                                                        requestDto.getStatuses(),
                                                                        requestDto.getLimit());
        };
    }

    public TransportDto getOrderTransport(User user, Long orderId, UUID transportId) {
        Transport transport = this.transportService.getTransportById(transportId);
        this.validateOrderTransportPermissions(user, orderId, transport);
        return new TransportDto(transport);
    }

    public void updateOrderTransport(User user, UpdateOrderTransportRequestDto requestDto) {
        Transport transport = this.transportService.getTransportById(requestDto.getTransportId());
        this.validateOrderTransportPermissions(user, requestDto.getOrderId(), transport);

        TransportDto updatedValues = new TransportDto(transport);

        updatedValues.setParkNumber(requestDto.getParkNumber());
        updatedValues.setBrand(requestDto.getBrand());
        updatedValues.setModel(requestDto.getModel());
        updatedValues.setVin(requestDto.getVin());
        updatedValues.setNumber(requestDto.getNumber());

        new UpdateTransportCommand(this.transportService, this.orderRepository).updateTransport(transport, updatedValues);
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

        boolean isAccessToExistingOrder = (order.getMaster() != null &&
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

    private void validateOrderTransportPermissions(User user, Long orderId, Transport transport) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        Order order = this.orderRepository.findById(orderId).orElseThrow();

        if (user.getRole() == UserRole.MASTER &&
            (order.getMaster() == null || !order.getMaster().getId().equals(user.getId())) &&
            order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<UUID> transportIds = order.getTransport().stream().map(Transport::getId).toList();
        if (!transportIds.contains(transport.getId()) &&
            !Objects.equals(transport.getCustomer().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}