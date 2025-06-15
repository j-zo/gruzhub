package ru.gruzhub.orders.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.orders.auto.dto.AutoResponseDto;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.users.dto.UserResponseDto;

@Data
@NoArgsConstructor
public class OrderResponseDto {
    private Long id;
    private String guaranteeUuid;
    private Long customerId;
    private UserResponseDto customer;
    private Long masterId;
    private UserResponseDto master;
    private Long driverId;
    private UserResponseDto driver;
    private List<AutoResponseDto> autos;
    private String description;
    private String notes;
    private Long createdAt;
    private Long updatedAt;
    private OrderStatus status;
    private Long lastStatusUpdateTime;
    private Address address;
    @JsonProperty("isNeedEvacuator")
    private boolean isNeedEvacuator;
    @JsonProperty("isNeedMobileTeam")
    private boolean isNeedMobileTeam;
    private String urgency;
    private List<Long> declinedMastersIds;

    public OrderResponseDto(Order order) {
        this.id = order.getId();
        this.guaranteeUuid = order.getGuaranteeUuid();

        if (order.getCustomer() != null) {
            this.customerId = order.getCustomer().getId();
            this.customer = new UserResponseDto(order.getCustomer());
        }

        if (order.getMaster() != null) {
            this.masterId = order.getMaster().getId();
            this.master = order.getMaster() != null ? new UserResponseDto(order.getMaster()) : null;
        }

        if (order.getDriver() != null) {
            this.driverId = order.getDriver().getId();
            this.driver = order.getDriver() != null ? new UserResponseDto(order.getDriver()) : null;
        }

        this.autos = new ArrayList<>();
        for (Auto auto : order.getAutos()) {
            this.autos.add(new AutoResponseDto(auto));
        }

        this.description = order.getDescription();
        this.notes = order.getNotes();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.status = order.getStatus();
        this.lastStatusUpdateTime = order.getLastStatusUpdateTime();
        this.address = order.getAddress();
        this.isNeedEvacuator = order.isNeedEvacuator();
        this.isNeedMobileTeam = order.isNeedMobileTeam();
        this.urgency = order.getUrgency();
        this.declinedMastersIds = order.getDeclinedMastersIds();
    }
}
