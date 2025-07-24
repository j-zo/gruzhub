package ru.gruzhub.transport.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "transport_column")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportColumn {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "column_number",
            columnDefinition = "TEXT")
    private String columnNumber;
}
