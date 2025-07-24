package ru.gruzhub.orders.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponseDto {
    private Long orderId;
    private UUID driverId;
}
