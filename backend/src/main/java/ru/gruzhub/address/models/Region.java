package ru.gruzhub.address.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name",
            columnDefinition = "TEXT")
    private String name;

    @Column(name = "codes",
            columnDefinition = "TEXT")
    private String codes;

    @ManyToOne
    @JoinColumn(name = "country_code",
                referencedColumnName = "code")
    private Country country;
}
