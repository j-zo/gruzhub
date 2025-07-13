package ru.gruzhub.mechanic.model;

import jakarta.persistence.*;
import lombok.*;
import ru.gruzhub.transport.model.TransportColumn;

@Entity
@Table(name = "mechanic")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mechanic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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
