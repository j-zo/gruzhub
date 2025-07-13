package ru.gruzhub.orders.tasks.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskResponseDto {
    private Long id;
    private Long transportId;
    private Long orderId;
    private String name;
    private String description;
    private BigDecimal price;
}
