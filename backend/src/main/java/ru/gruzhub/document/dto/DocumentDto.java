package ru.gruzhub.document.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.document.model.Document;

@Getter
@Setter
@NoArgsConstructor
public class DocumentDto {

    private Long id;

    public DocumentDto(Document document) {
        this.id = document.getId();
    }
}
