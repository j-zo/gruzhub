package ru.gruzhub.users.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.users.enums.UserRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUsersRequestDto {
    private List<UserRole> roles;
    private List<Long> regionsIds;
}
