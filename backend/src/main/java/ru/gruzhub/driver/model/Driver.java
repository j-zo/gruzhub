package ru.gruzhub.driver.model;

import jakarta.persistence.*;
import lombok.*;
import ru.gruzhub.document.model.Document;

import java.util.List;

@Entity
@Table(name = "driver")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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

    @OneToMany(mappedBy = "owner_driver_id", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents;
}