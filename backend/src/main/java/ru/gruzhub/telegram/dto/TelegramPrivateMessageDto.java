package ru.gruzhub.telegram.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class TelegramPrivateMessageDto {
    @Nullable
    private String text;

    @NonNull
    private TelegramUserDto userDto;
}
