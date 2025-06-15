package ru.gruzhub.users.models;

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

@Getter
@Setter
@Entity
@Table(name = "user_info_changes")
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",
                nullable = false)
    private User user;

    @Column(name = "previous_name",
            nullable = false,
            columnDefinition = "TEXT")
    private String previousName;

    @Column(name = "new_name",
            nullable = false,
            columnDefinition = "TEXT")
    private String newName;

    @Column(name = "previous_phone",
            columnDefinition = "TEXT")
    private String previousPhone;

    @Column(name = "new_phone",
            columnDefinition = "TEXT")
    private String newPhone;

    @Column(name = "previous_email",
            columnDefinition = "TEXT")
    private String previousEmail;

    @Column(name = "new_email",
            columnDefinition = "TEXT")
    private String newEmail;

    @Column(name = "previous_inn",
            columnDefinition = "TEXT")
    private String previousInn;

    @Column(name = "new_inn",
            columnDefinition = "TEXT")
    private String newInn;

    @Column(name = "date",
            nullable = false)
    private Long date;
}
