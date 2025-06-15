package ru.gruzhub.orders.auto.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.orders.auto.enums.AutoType;
import ru.gruzhub.users.models.User;

@Entity
@Table(name = "auto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;

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

    @Column(name = "is_merged",
            nullable = false)
    private boolean isMerged;

    @ManyToOne
    @JoinColumn(name = "merged_to_id",
                referencedColumnName = "id")
    private Auto mergedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",
            nullable = false,
            columnDefinition = "TEXT")
    private AutoType type;
}
