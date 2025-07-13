package ru.gruzhub.document.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "filename",
            columnDefinition = "VARCHAR(255)")
    private String filename;

    @Column(name = "filepath",
            columnDefinition = "VARCHAR(500)")
    private String filepath;

    @Column(name = "uploaded_at",
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadedAt;
}
