package ru.gruzhub.orders.orders.dto;

import lombok.Data;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.users.dto.UserDto;

@Data
public class OrderStatusChangeDto {
    private Long id;
    private Long updatedAt;
    private Long orderId;
    private OrderStatus newStatus;
    private UserDto updatedBy;
    private UserDto master;
    private String comment;
}
