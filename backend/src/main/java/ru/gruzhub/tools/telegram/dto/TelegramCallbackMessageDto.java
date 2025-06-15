package ru.gruzhub.tools.telegram.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class TelegramCallbackMessageDto {
  @Nullable private String text;

  @NonNull private Long inlineMessageId;

  @NonNull private TelegramUserDto userDto;
}
