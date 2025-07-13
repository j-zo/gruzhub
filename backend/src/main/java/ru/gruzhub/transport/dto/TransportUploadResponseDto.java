package ru.gruzhub.transport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.transport.enums.TransportUploadStatus;

@Getter
@Setter
@NoArgsConstructor
public class TransportUploadResponseDto {

    private Long transportId;
    private TransportUploadStatus status;
    private String message;

    public TransportUploadResponseDto(Long transportId, TransportUploadStatus status, String message) {
        this.transportId = transportId;
        this.status = status;
        this.message = message;
    }
}
