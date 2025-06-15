package ru.gruzhub.address.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "address",
       indexes = {@Index(name = "idx_address_region_id",
                         columnList = "region_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "city",
            columnDefinition = "TEXT")
    private String city;

    @Column(name = "street",
            columnDefinition = "TEXT")
    private String street;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longtitude")
    private Double longitude;
}
