package ru.gruzhub.orders.orders.statistics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.orders.orders.statistics.dto.OrderCreationDto;
import ru.gruzhub.orders.orders.statistics.enums.OrderStatisticsPeriod;

@Service
@RequiredArgsConstructor
public class OrderStatisticsService {
    private final OrderStatisticsRepository orderStatisticsRepository;

    public List<OrderCreationDto> getOrderCreationDates(OrderStatisticsPeriod period) {
        List<Object[]> stats = switch (period) {
            case DAY -> this.findOrdersByDay();
            case WEEK -> this.findOrdersByWeek();
            case MONTH -> this.findOrdersByMonth();
        };

        return this.mapResults(stats);
    }

    private List<Object[]> findOrdersByDay() {
        return this.orderStatisticsRepository.findOrdersByDay();
    }

    private List<Object[]> findOrdersByWeek() {
        return this.orderStatisticsRepository.findOrdersByWeek();
    }

    private List<Object[]> findOrdersByMonth() {
        return this.orderStatisticsRepository.findOrdersByMonth();
    }

    private List<OrderCreationDto> mapResults(List<Object[]> results) {
        return results.stream().map(row -> {
            Instant instant = (Instant) row[0];
            LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate(); // Convert to UTC
            String period = date.toString(); // Format as 'YYYY-MM-DD'
            return new OrderCreationDto(period, ((Number) row[1]).longValue());
        }).collect(Collectors.toList());
    }
}

