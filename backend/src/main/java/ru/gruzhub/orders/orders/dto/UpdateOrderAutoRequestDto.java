package ru.gruzhub.orders.orders.dto;

import lombok.Data;

@Data
public class UpdateOrderAutoRequestDto {
    private Long orderId;
    private Long autoId;
    private String brand;
    private String model;
    private String vin;
    private String number;
}
