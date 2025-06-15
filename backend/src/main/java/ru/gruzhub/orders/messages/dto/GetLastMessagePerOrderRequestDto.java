package ru.gruzhub.orders.messages.dto;

import java.util.List;
import lombok.Data;

@Data
public class GetLastMessagePerOrderRequestDto {
    private List<Long> ordersIds;
}
