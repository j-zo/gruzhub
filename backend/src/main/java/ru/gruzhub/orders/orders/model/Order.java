package ru.gruzhub.orders.orders.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.driver.model.Driver;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.users.models.User;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id",
            nullable = false)
    private Long id;

    @Column(name = "guarantee_uuid",
            nullable = false,
            columnDefinition = "TEXT")
    private String guaranteeUuid;

    @Nullable
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @Nullable
    @Setter
    @ManyToOne
    @JoinColumn(name = "master_id")
    private User master;

    @Column(name = "declined_masters_ids",
            columnDefinition = "TEXT")
    private String declinedMastersIdsArray;

    @Transient
    private List<Long> declinedMastersIds;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToMany(cascade = {CascadeType.DETACH,
                           CascadeType.MERGE,
                           CascadeType.PERSIST,
                           CascadeType.REFRESH},
                fetch = FetchType.EAGER)
    @JoinTable(name = "order_to_transport_assosiation",
               joinColumns = @JoinColumn(name = "order_id"),
               inverseJoinColumns = @JoinColumn(name = "transport_id"))
    private List<Transport> transport;

    @Column(name = "created_at",
            nullable = false)
    private Long createdAt;

    @Column(name = "updated_at",
            nullable = false)
    private Long updatedAt;

    @Setter
    @Column(name = "last_status_update_time",
            nullable = false)
    private Long lastStatusUpdateTime;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            nullable = false,
            columnDefinition = "TEXT")
    private OrderStatus status;

    @Column(name = "description",
            columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes",
            columnDefinition = "TEXT")
    private String notes;

    @ManyToOne
    @JoinColumn(name = "address_id",
                nullable = false)
    private Address address;

    @JsonProperty("isNeedEvacuator")
    @Column(name = "is_need_evacuator",
            nullable = false)
    private boolean isNeedEvacuator;

    @JsonProperty("isNeedMobileTeam")
    @Column(name = "is_need_mobile_team",
            nullable = false)
    private boolean isNeedMobileTeam;

    @Column(name = "urgency",
            columnDefinition = "TEXT")
    private String urgency;

    @PostLoad
    public void postLoad() {
        if (this.declinedMastersIdsArray != null && !this.declinedMastersIdsArray.isEmpty()) {
            this.declinedMastersIds =
                new ArrayList<>(Stream.of(this.declinedMastersIdsArray.split(","))
                                      .map(Long::parseLong)
                                      .toList());
        } else {
            this.declinedMastersIds = new ArrayList<>();
        }
    }

    public void addDeclinedMaster(Long masterId) {
        this.declinedMastersIds.add(masterId);
        this.declinedMastersIdsArray =
            String.join(",", this.declinedMastersIds.stream().map(String::valueOf).toList());
    }
}
