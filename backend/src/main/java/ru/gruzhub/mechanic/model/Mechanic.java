package ru.gruzhub.mechanic.model;

import jakarta.persistence.*;
import lombok.*;
import ru.gruzhub.transport.model.TransportColumn;

import java.util.UUID;

@Entity
@Table(name = "mechanic")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mechanic {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "transport_column_id")
    private TransportColumn transportColumn;

    @Column(name = "name",
            nullable = false,
            columnDefinition = "TEXT")
    private String name;

    @Column(name = "phone",
            columnDefinition = "TEXT")
    private String phone;

    @Column(name = "email",
            columnDefinition = "TEXT")
    private String email;
}
