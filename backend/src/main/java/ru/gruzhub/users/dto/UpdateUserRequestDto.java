package ru.gruzhub.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {
    private Long id;

    private String name;
    private String inn;

    private String email;
    private String phone;
    private String password;

    private Integer tripRadiusKm;

    private Long regionId;
    private String city;
    private String street;
}
