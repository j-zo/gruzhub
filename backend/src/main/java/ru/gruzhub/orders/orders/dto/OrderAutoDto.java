package ru.gruzhub.orders.orders.dto;

import lombok.Data;
import ru.gruzhub.orders.auto.enums.AutoType;

@Data
public class OrderAutoDto {
    private Long autoId;
    private String brand;
    private String model;
    private String vin;
    private String number;
    private AutoType type;
}
