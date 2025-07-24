package ru.gruzhub.orders.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.driver.model.Driver;
import ru.gruzhub.transport.dto.DriverDto;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.users.dto.UserDto;

@Data
@NoArgsConstructor
public class OrderResponseDto {
    private Long id;
    private String guaranteeUuid;
    private Long customerId;
    private UserDto customer;
    private Long masterId;
    private UserDto master;
    private UUID driverId;
    private DriverDto driver;
    private List<TransportDto> transports;
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
            this.customer = new UserDto(order.getCustomer());
        }

        if (order.getMaster() != null) {
            this.masterId = order.getMaster().getId();
            this.master = order.getMaster() != null ? new UserDto(order.getMaster()) : null;
        }

        if (order.getDriver() != null) {
            this.driverId = order.getDriver().getId();
            this.driver = order.getDriver() != null ? new DriverDto(order.getDriver()) : null;
        }

        this.transports = new ArrayList<>();
        for (Transport transport : order.getTransport()) {
            this.transports.add(new TransportDto(transport));
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
