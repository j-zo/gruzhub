package ru.gruzhub.orders.orders.statistics;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.orders.orders.statistics.dto.OrderCreationDto;
import ru.gruzhub.orders.orders.statistics.enums.OrderStatisticsPeriod;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;

@RestController
@RequestMapping("/orders-statistics")
@RequiredArgsConstructor
public class OrderStatisticsContoller {
    private final OrderStatisticsService orderStatisticsService;
    private final UsersService usersService;

    @GetMapping("/orders")
    public List<OrderCreationDto> getOrdersStatistics(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestParam OrderStatisticsPeriod period) {
        this.usersService.validateAuthRole(authorization, List.of(UserRole.ADMIN));
        return this.orderStatisticsService.getOrderCreationDates(period);
    }
}
