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
public class CreateUserRequestDto {
    private String name;
    private String inn;
    private UserRole role;

    private String email;
    private String phone;
    private String password;

    private Integer tripRadiusKm;

    private Long regionId;
    private String city;
    private String street;
}
