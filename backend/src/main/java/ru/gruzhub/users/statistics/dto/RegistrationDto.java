package ru.gruzhub.users.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationDto {
    private String date;
    private Long count;
}