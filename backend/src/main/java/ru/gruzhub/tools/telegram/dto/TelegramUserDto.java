package ru.gruzhub.tools.telegram.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class TelegramUserDto {
  @NonNull private Long userTgId;

  @Nullable private String tgUsername;

  @Nullable private String tgName;
}
