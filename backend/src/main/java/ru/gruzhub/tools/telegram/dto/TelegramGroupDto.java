package ru.gruzhub.tools.telegram.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class TelegramGroupDto {
  @NonNull private Long chatId;

  @Nullable private String title;

  @Nullable private String username;

  @NonNull private String type;
}
