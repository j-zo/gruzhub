package ru.gruzhub.orders.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponseDto {
    private Long orderId;
    private Long driverId;
}
