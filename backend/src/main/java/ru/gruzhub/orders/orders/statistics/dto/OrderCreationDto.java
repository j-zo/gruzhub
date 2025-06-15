package ru.gruzhub.orders.orders.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderCreationDto {
    private String date;
    private Long count;
}