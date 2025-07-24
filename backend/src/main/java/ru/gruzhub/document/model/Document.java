package ru.gruzhub.document.model;

import jakarta.persistence.*;
import lombok.*;
import ru.gruzhub.driver.model.Driver;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.transport.model.TransportColumn;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "filename",
            columnDefinition = "VARCHAR(255)")
    private String filename;

    @Column(name = "filepath",
            columnDefinition = "VARCHAR(500)")
    private String filepath;

    @Column(name = "uploaded_at",
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "owner_driver_id")
    private Driver driverOwner;

    @ManyToOne
    @JoinColumn(name = "owner_transport_id")
    private Transport transportOwner;
}
