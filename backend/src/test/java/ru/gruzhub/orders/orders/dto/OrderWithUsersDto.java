package ru.gruzhub.orders.orders.dto;

public class OrderWithUsersDto {
    private final Long orderId;

    public OrderWithUsersDto(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return this.orderId;
    }
}
