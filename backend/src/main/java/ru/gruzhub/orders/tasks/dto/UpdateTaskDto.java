package ru.gruzhub.orders.tasks.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateTaskDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
}
