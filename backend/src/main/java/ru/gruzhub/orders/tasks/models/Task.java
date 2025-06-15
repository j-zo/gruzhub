package ru.gruzhub.orders.tasks.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.tasks.dto.TaskResponseDto;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",
            nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER,
               optional = false)
    @JoinColumn(name = "auto_id",
                nullable = false)
    private Auto auto;

    @ManyToOne(fetch = FetchType.EAGER,
               optional = false)
    @JoinColumn(name = "order_id",
                nullable = false)
    private Order order;

    @Column(name = "name",
            nullable = false,
            columnDefinition = "TEXT")
    private String name;

    @Column(name = "description",
            columnDefinition = "TEXT")
    private String description;

    @Column(name = "price",
            precision = 12,
            scale = 2)
    private BigDecimal price;

    @Column(name = "created_at",
            nullable = false)
    private Long createdAt;

    @Column(name = "updated_at",
            nullable = false)
    private Long updatedAt;

    public TaskResponseDto toDto() {
        return TaskResponseDto.builder()
                              .id(this.id)
                              .autoId(this.auto.getId())
                              .orderId(this.order.getId())
                              .name(this.name)
                              .description(this.description)
                              .price(this.price)
                              .build();
    }
}
