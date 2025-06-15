package ru.gruzhub.tools.telegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelegramGroupMigrationDto {
  private Long originalTgId;
  private Long migratedToTgId;
}
