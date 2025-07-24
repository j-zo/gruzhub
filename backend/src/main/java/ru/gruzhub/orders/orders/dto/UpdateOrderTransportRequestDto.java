package ru.gruzhub.orders.orders.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateOrderTransportRequestDto {
    private Long orderId;
    private UUID transportId;
    private String brand;
    private String model;
    private String vin;
    private String number;
    private String parkNumber;
}
