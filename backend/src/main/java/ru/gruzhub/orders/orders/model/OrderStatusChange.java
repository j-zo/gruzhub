package ru.gruzhub.orders.orders.model;

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
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.users.models.User;

@Entity
@Table(name = "orders_status_changes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",
            nullable = false)
    private Long id;

    @Column(name = "updated_at",
            nullable = false)
    private Long updatedAt;

    @ManyToOne
    @JoinColumn(name = "order_id",
                nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status",
            nullable = false,
            columnDefinition = "TEXT")
    private OrderStatus newStatus;

    @ManyToOne
    @JoinColumn(name = "updated_by_id",
                nullable = false)
    private User updatedBy;

    @ManyToOne
    @JoinColumn(name = "master_id")
    private User master;

    @Column(name = "comment",
            columnDefinition = "TEXT")
    private String comment;
}
