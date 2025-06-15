package ru.gruzhub.orders.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class CreateOrderRequestDto {
    private String guaranteeUuid;
    private String driverName;
    private String driverPhone;
    private String driverEmail;
    private List<OrderAutoDto> autos;
    private Long regionId;
    private String city;
    private String street;
    private String description;
    private String notes;
    @JsonProperty("isNeedEvacuator")
    private boolean isNeedEvacuator;
    @JsonProperty("isNeedMobileTeam")
    private boolean isNeedMobileTeam;
    private String urgency;
}
