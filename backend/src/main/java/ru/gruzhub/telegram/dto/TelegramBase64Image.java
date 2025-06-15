package ru.gruzhub.telegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelegramBase64Image {
    private String fileId;
    private String base64;
    private String base64Type;
    private String fileExtension;
}
