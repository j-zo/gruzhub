package ru.gruzhub.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.users.enums.UserRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignInUserRequestDto {
    private String email;
    private String phone;
    private UserRole role;
    private String password;
}
