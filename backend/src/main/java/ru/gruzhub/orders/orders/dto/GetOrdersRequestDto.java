package ru.gruzhub.orders.orders.dto;

import java.util.List;
import lombok.Data;
import ru.gruzhub.orders.orders.enums.OrderStatus;

@Data
public class GetOrdersRequestDto {
    private List<OrderStatus> statuses;
    private Long masterId;
    private Long customerId;
    private Long driverId;
    private Long transportId;
    private List<Long> regionsIds;
    private Long userId;
    private Integer limit;
}
