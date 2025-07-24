package ru.gruzhub.driver.model;

import jakarta.persistence.*;
import lombok.*;
import ru.gruzhub.document.model.Document;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "driver")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

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

    @OneToMany(mappedBy = "driverOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents;
}