package ru.gruzhub.transport.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.document.model.Document;
import ru.gruzhub.driver.model.Driver;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.users.models.User;

import java.util.List;

@Entity
@Table(name = "transport")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "main_transport_id")
    private Transport mainTransport;

    @ManyToOne
    @JoinColumn(name = "transport_column_id")
    private TransportColumn transportColumn;

    @OneToMany(mappedBy = "owner_transport_id", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents;

    @Column(name = "brand",
            columnDefinition = "TEXT")
    private String brand;

    @Column(name = "model",
            columnDefinition = "TEXT")
    private String model;

    @Column(name = "vin",
            columnDefinition = "TEXT")
    private String vin;

    @Column(name = "number",
            columnDefinition = "TEXT")
    private String number;

    @Column(name = "parkNumber",
            columnDefinition = "TEXT")
    private String parkNumber;

    @Column(name = "is_merged",
            nullable = false)
    private boolean isMerged;

    @ManyToOne
    @JoinColumn(name = "merged_to_id",
                referencedColumnName = "id")
    private Transport mergedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",
            nullable = false,
            columnDefinition = "TEXT")
    private TransportType type;
}
