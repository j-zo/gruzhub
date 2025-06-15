package ru.gruzhub.orders.orders.dto;

import ru.gruzhub.users.testing.dto.TestAuthDataDto;

public class AuthWithOrderDto {
    private final TestAuthDataDto authData;
    private final OrderResponseDto order;

    public AuthWithOrderDto(TestAuthDataDto authData, OrderResponseDto order) {
        this.authData = authData;
        this.order = order;
    }

    public TestAuthDataDto getAuthData() {
        return authData;
    }

    public OrderResponseDto getOrder() {
        return order;
    }
}
