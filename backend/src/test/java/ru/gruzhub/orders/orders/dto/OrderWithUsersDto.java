package ru.gruzhub.orders.orders.dto;

public class OrderWithUsersDto {
    private final Long orderId;
    private final String orderOwnerToken;
    private final String masterToken;

    public OrderWithUsersDto(Long orderId, String orderOwnerToken, String masterToken) {
        this.orderId = orderId;
        this.orderOwnerToken = orderOwnerToken;
        this.masterToken = masterToken;
    }

    public Long getOrderId() {
        return this.orderId;
    }

    public String getOrderOwnerToken() {
        return this.orderOwnerToken;
    }

    public String getMasterToken() {
        return this.masterToken;
    }
}
