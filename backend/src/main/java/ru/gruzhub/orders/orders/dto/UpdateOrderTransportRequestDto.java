package ru.gruzhub.orders.orders.dto;

import lombok.Data;

@Data
public class UpdateOrderTransportRequestDto {
    private Long orderId;
    private Long transportId;
    private String brand;
    private String model;
    private String vin;
    private String number;
    private String parkNumber;
}
