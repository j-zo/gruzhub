package ru.gruzhub.telegram.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class TelegramGroupDto {
    @NonNull
    private Long chatId;

    @Nullable
    private String title;

    @NonNull
    private String type;
}
