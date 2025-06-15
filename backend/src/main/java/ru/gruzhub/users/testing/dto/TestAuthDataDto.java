package ru.gruzhub.users.testing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestAuthDataDto {
    private Long userId;
    private String accessToken;
    private String email;
}
