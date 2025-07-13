package ru.gruzhub.orders.notifications.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.orders.orders.model.Order;

@Entity
@Table(name = "orders_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OldOrderNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",
            nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id",
                nullable = false)
    private Order order;

    @Column(name = "order_last_status_update_time",
            nullable = false)
    private Long orderLastStatusUpdateTime;

    @Column(name = "created_at",
            nullable = false)
    private ZonedDateTime createdAt;
}
