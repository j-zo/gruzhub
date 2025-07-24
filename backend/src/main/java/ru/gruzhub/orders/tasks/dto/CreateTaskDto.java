package ru.gruzhub.orders.tasks.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTaskDto {
    private UUID transportId;
    private Long orderId;
    private String name;
    private String description;
    private BigDecimal price;
}
