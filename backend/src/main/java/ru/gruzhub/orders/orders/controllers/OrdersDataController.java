package ru.gruzhub.orders.orders.controllers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.orders.auto.dto.AutoResponseDto;
import ru.gruzhub.orders.orders.dto.GetOrdersRequestDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.dto.UpdateOrderAutoRequestDto;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.models.OrderStatusChange;
import ru.gruzhub.orders.orders.services.OrdersDataService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.models.UserInfoChange;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersDataController {
    private final OrdersDataService dataService;
    private final UsersService usersService;

    @PostMapping("/orders")
    public List<OrderResponseDto> getOrders(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestBody GetOrdersRequestDto getOrdersRequest) {
        User user = this.usersService.getUserFromToken(authorization);
        List<Order> orders = this.dataService.getOrders(user, getOrdersRequest);
        return orders.stream().map(OrderResponseDto::new).toList();
    }

    @GetMapping("/auto")
    public AutoResponseDto getOrderAuto(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestParam Long orderId,
        @RequestParam Long autoId) {
        User user = this.usersService.getUserFromToken(authorization);
        return this.dataService.getOrderAuto(user, orderId, autoId);
    }

    @PostMapping("/auto")
    public ResponseEntity<Void> updateOrderAuto(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestBody UpdateOrderAutoRequestDto updateAutoRequest) {
        User user = this.usersService.getUserFromToken(authorization);
        this.dataService.updateOrderAuto(user, updateAutoRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/auto/{autoId}")
    public List<OrderResponseDto> getAutoOrders(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @PathVariable Long autoId) {
        User user = this.usersService.getUserFromToken(authorization);
        return this.dataService.getAutoOrders(user, autoId);
    }

    @GetMapping("/{orderId}")
    public OrderResponseDto getOrderById(@PathVariable Long orderId,
                                         @RequestHeader(HttpHeaders.AUTHORIZATION)
                                         String authorization) {

        User user = this.usersService.getUserFromToken(authorization);
        Order order = this.dataService.getOrderById(user, orderId);
        return new OrderResponseDto(order);
    }

    @GetMapping("/order-status-changes/{orderId}")
    public List<OrderStatusChange> getOrderStatusChanges(@PathVariable Long orderId,
                                                         @RequestHeader(HttpHeaders.AUTHORIZATION)
                                                         String authorization) {
        User user = this.usersService.getUserFromToken(authorization);
        return this.dataService.getOrderStatusChanges(user, orderId);
    }

    @GetMapping("/user-changes")
    public List<UserInfoChange> getUserChangesByOrderId(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestParam Long orderId,
        @RequestParam Long userId) {
        User user = this.usersService.getUserFromToken(authorization);
        return this.dataService.getUserInfoChanges(user, orderId, userId);
    }
}
