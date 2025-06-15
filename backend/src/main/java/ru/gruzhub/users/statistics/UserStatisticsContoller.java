package ru.gruzhub.users.statistics;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.statistics.dto.RegistrationDto;
import ru.gruzhub.users.statistics.enums.UserStatisticsPeriod;

@RestController
@RequestMapping("/users-statistics")
@RequiredArgsConstructor
public class UserStatisticsContoller {
    private final UserStatisticsService userStatisticsService;
    private final UsersService usersService;

    @GetMapping("/registrations")
    public List<RegistrationDto> getRegistrations(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
        @RequestParam UserStatisticsPeriod period) {
        this.usersService.validateAuthRole(authorization, List.of(UserRole.ADMIN));
        return this.userStatisticsService.getRegistrations(period);
    }
}
