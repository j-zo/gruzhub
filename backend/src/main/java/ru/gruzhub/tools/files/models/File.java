package ru.gruzhub.tools.files.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.tools.files.enums.FileType;
import ru.gruzhub.users.models.User;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {
    @Id
    @Column(name = "code",
            nullable = false,
            unique = true,
            columnDefinition = "TEXT")
    private String code;

    @Column(name = "location",
            nullable = false,
            columnDefinition = "TEXT")
    private String location;

    @Column(name = "filename",
            nullable = false,
            columnDefinition = "TEXT")
    private String filename;

    @Column(name = "extension",
            nullable = false,
            columnDefinition = "TEXT")
    private String extension;

    @Column(name = "content_type",
            nullable = false,
            columnDefinition = "TEXT")
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",
            nullable = false,
            columnDefinition = "TEXT")
    private FileType type;

    @Column(name = "file_size_bytes",
            nullable = false)
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id",
                nullable = false)
    private User user;

    @Column(name = "create_at",
            nullable = false)
    private Long createdAt;
}
