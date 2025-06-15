package ru.gruzhub.address.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "country",
       indexes = {@Index(name = "idx_country_code",
                         columnList = "code")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Country {
    @Id
    @Column(name = "code",
            columnDefinition = "TEXT",
            nullable = false)
    private String code;

    @Column(name = "name",
            columnDefinition = "TEXT",
            nullable = false)
    private String name;
}
