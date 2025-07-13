package ru.gruzhub.orders.messages.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.tools.files.models.File;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Entity
@Table(name = "order_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guarantee_id",
            nullable = false,
            columnDefinition = "TEXT")
    private String guaranteeId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id",
                nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id",
                nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role",
            nullable = false,
            columnDefinition = "TEXT")
    private UserRole userRole;

    @Column(name = "text",
            columnDefinition = "TEXT")
    private String text;

    @Column(name = "date",
            nullable = false)
    private Long date;

    @Column(name = "file_code",
            columnDefinition = "TEXT")
    private String fileCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_code",
                referencedColumnName = "code",
                insertable = false,
                updatable = false)
    private File file;

    @Column(name = "is_viewed_by_master",
            nullable = false)
    private boolean isViewedByMaster;

    @Column(name = "is_viewed_by_driver",
            nullable = false)
    private boolean isViewedByDriver;

    @Column(name = "is_viewed_by_customer",
            nullable = false)
    private boolean isViewedByCustomer;
}
