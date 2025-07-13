package ru.gruzhub.orders.orders.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.orders.orders.dto.GetOrdersRequestDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.dto.UpdateOrderTransportRequestDto;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.model.OrderStatusChange;
import ru.gruzhub.orders.orders.service.OrdersDataService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.models.UserInfoChange;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class OrdersDataController {
    private final OrdersDataService dataService;
    private final UsersService usersService;

    @PostMapping("/orders")
    public List<OrderResponseDto> getOrders(@RequestBody GetOrdersRequestDto getOrdersRequest) {
        List<Order> orders = this.dataService.getOrders(this.usersService.getCurrentUser(), getOrdersRequest);
        return orders.stream().map(OrderResponseDto::new).toList();
    }

    @GetMapping("/transport")
    public TransportDto getOrderTransport(
        @RequestParam Long orderId,
        @RequestParam Long transportId) {
        return this.dataService.getOrderTransport(this.usersService.getCurrentUser(), orderId, transportId);
    }

    @PostMapping("/transport")
    public ResponseEntity<Void> updateOrderTransport(@RequestBody UpdateOrderTransportRequestDto updateTransportRequest) {
        this.dataService.updateOrderTransport(this.usersService.getCurrentUser(), updateTransportRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/transport/{transportId}")
    public List<OrderResponseDto> getTransportOrders(@PathVariable Long transportId) {
        return this.dataService.getTransportOrders(this.usersService.getCurrentUser(), transportId);
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
