package ru.gruzhub.orders.orders.controllers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import ru.gruzhub.users.models.UserInfoChange;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersDataController {
    private final OrdersDataService dataService;
    private final UsersService usersService;

    @PostMapping("/orders")
    public List<OrderResponseDto> getOrders(@RequestBody GetOrdersRequestDto getOrdersRequest) {
        List<Order> orders = this.dataService.getOrders(this.usersService.getCurrentUser(), getOrdersRequest);
        return orders.stream().map(OrderResponseDto::new).toList();
    }

    @GetMapping("/auto")
    public AutoResponseDto getOrderAuto(
        @RequestParam Long orderId,
        @RequestParam Long autoId) {
        return this.dataService.getOrderAuto(this.usersService.getCurrentUser(), orderId, autoId);
    }

    @PostMapping("/auto")
    public ResponseEntity<Void> updateOrderAuto(@RequestBody UpdateOrderAutoRequestDto updateAutoRequest) {
        this.dataService.updateOrderAuto(this.usersService.getCurrentUser(), updateAutoRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/auto/{autoId}")
    public List<OrderResponseDto> getAutoOrders(@PathVariable Long autoId) {
        return this.dataService.getAutoOrders(this.usersService.getCurrentUser(), autoId);
    }

    @GetMapping("/{orderId}")
    public OrderResponseDto getOrderById(@PathVariable Long orderId) {

        Order order = this.dataService.getOrderById(this.usersService.getCurrentUser(), orderId);
        return new OrderResponseDto(order);
    }

    @GetMapping("/order-status-changes/{orderId}")
    public List<OrderStatusChange> getOrderStatusChanges(@PathVariable Long orderId) {
        return this.dataService.getOrderStatusChanges(this.usersService.getCurrentUser(), orderId);
    }

    @GetMapping("/user-changes")
    public List<UserInfoChange> getUserChangesByOrderId(
        @RequestParam Long orderId,
        @RequestParam Long userId) {
        return this.dataService.getUserInfoChanges(this.usersService.getCurrentUser(), orderId, userId);
    }
}
