package ru.gruzhub.users.statistics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.users.statistics.dto.RegistrationDto;
import ru.gruzhub.users.statistics.enums.UserStatisticsPeriod;

@Service
@RequiredArgsConstructor
public class UserStatisticsService {
    private final UserStatisticsRepository userStatisticsRepository;

    public List<RegistrationDto> getRegistrations(UserStatisticsPeriod period) {
        List<Object[]> registrations = switch (period) {
            case DAY -> this.findRegistrationsByDay();
            case WEEK -> this.findRegistrationsByWeek();
            case MONTH -> this.findRegistrationsByMonth();
        };

        return this.mapResults(registrations);
    }

    private List<Object[]> findRegistrationsByDay() {
        return this.userStatisticsRepository.findRegistrationsByDay();
    }

    private List<Object[]> findRegistrationsByWeek() {
        return this.userStatisticsRepository.findRegistrationsByWeek();
    }

    private List<Object[]> findRegistrationsByMonth() {
        return this.userStatisticsRepository.findRegistrationsByMonth();
    }

    private List<RegistrationDto> mapResults(List<Object[]> results) {
        return results.stream().map(row -> {
            Instant instant = (Instant) row[0];
            LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate(); // Convert to UTC
            String period = date.toString(); // Format as 'YYYY-MM-DD'
            return new RegistrationDto(period, ((Number) row[1]).longValue());
        }).collect(Collectors.toList());
    }
}
