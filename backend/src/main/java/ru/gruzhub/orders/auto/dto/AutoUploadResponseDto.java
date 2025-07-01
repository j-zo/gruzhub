package ru.gruzhub.orders.auto.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.orders.auto.enums.AutoUploadStatus;

@Getter
@Setter
@NoArgsConstructor
public class AutoUploadResponseDto {

    private Long autoId;
    private AutoUploadStatus status;
    private String message;

    public AutoUploadResponseDto(Long autoId, AutoUploadStatus status, String message) {
        this.autoId = autoId;
        this.status = status;
        this.message = message;
    }
}
