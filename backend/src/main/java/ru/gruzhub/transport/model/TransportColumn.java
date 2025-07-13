package ru.gruzhub.transport.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_column")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "column_number",
            columnDefinition = "TEXT")
    private String columnNumber;
}
