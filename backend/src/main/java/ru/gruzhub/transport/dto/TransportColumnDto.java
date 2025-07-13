package ru.gruzhub.transport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.transport.model.TransportColumn;

@Getter
@Setter
@NoArgsConstructor
public class TransportColumnDto {

    private Long id;
    private String columnNumber;

    public TransportColumnDto(TransportColumn column) {
        this.id = column.getId();
        this.columnNumber = column.getColumnNumber();
    }
}
