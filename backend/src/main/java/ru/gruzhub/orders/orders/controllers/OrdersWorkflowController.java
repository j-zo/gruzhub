package ru.gruzhub.orders.orders.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.dto.DeclineOrderRequestDto;
import ru.gruzhub.orders.orders.services.OrdersWorkflowService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.models.User;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersWorkflowController {
    private final OrdersWorkflowService workflowService;
    private final UsersService usersService;

    @PostMapping("/create")
    public CreateOrderResponseDto createOrder(@RequestHeader(value = HttpHeaders.AUTHORIZATION,
                                                             required = false) String authorization,
                                              @RequestBody
                                              CreateOrderRequestDto createOrderRequest) {
        return this.workflowService.createOrder(authorization, createOrderRequest);
    }

    @GetMapping("/{orderId}/start_calculation_by_master")
    public ResponseEntity<?> startCalculationByMaster(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @PathVariable Long orderId) {
        User user = this.usersService.getUserFromToken(authorization);
        this.workflowService.startCalculationByMaster(user, orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}/decline_order_master")
    public void declineOrderMaster(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                   @PathVariable Long orderId,
                                   @RequestBody DeclineOrderRequestDto declineRequest) {
        try {
            User user = this.usersService.getUserFromToken(authorization);
            this.workflowService.declineOrderMaster(user, orderId, declineRequest.getComment());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{orderId}/send_for_confirmation_by_master")
    public void sendForConfirmationByMaster(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @PathVariable Long orderId) {
        User user = this.usersService.getUserFromToken(authorization);
        this.workflowService.sendForConfirmationByMaster(user, orderId);
    }

    @GetMapping("/{orderId}/accept_by_customer")
    public void acceptByCustomer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                 @PathVariable Long orderId) {
        User user = this.usersService.getUserFromToken(authorization);
        this.workflowService.acceptByCustomer(user, orderId);
    }

    @GetMapping("/{orderId}/complete_order")
    public void completeOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                              @PathVariable Long orderId) {
        User user = this.usersService.getUserFromToken(authorization);
        this.workflowService.completeOrder(user, orderId);
    }

    @PostMapping("/{orderId}/cancel_order")
    public void cancelOrder(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                            @PathVariable Long orderId,
                            @RequestBody DeclineOrderRequestDto cancelRequest) {
        User user = this.usersService.getUserFromToken(authorization);
        this.workflowService.cancelOrder(user, orderId, cancelRequest.getComment());
    }
}
