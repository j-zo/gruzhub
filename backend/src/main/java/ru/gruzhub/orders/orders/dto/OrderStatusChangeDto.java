package ru.gruzhub.orders.orders.dto;

import lombok.Data;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.users.dto.UserResponseDto;

@Data
public class OrderStatusChangeDto {
    private Long id;
    private Long updatedAt;
    private Long orderId;
    private OrderStatus newStatus;
    private UserResponseDto updatedBy;
    private UserResponseDto master;
    private String comment;
}
