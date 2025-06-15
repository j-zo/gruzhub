package ru.gruzhub.orders.messages.dto;

import lombok.Data;

@Data
public class SendMessageRequestDto {
    private String guaranteeId;
    private Long orderId;
    private String text;
}
