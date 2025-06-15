package ru.gruzhub.orders.auto.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.orders.auto.enums.AutoType;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.users.dto.UserResponseDto;

@Getter
@Setter
@NoArgsConstructor
public class AutoResponseDto {
    private Long id;
    private AutoType type;
    private UserResponseDto customer;
    private UserResponseDto driver;
    private String brand;
    private String model;
    private String vin;
    private String number;

    public AutoResponseDto(Auto auto) {
        this.id = auto.getId();
        this.type = auto.getType();

        if (customer != null) {
            this.customer = new UserResponseDto(auto.getCustomer());
        }

        if (driver != null) {
            this.driver = auto.getDriver() != null ? new UserResponseDto(auto.getDriver()) : null;
        }
        
        this.brand = auto.getBrand();
        this.model = auto.getModel();
        this.vin = auto.getVin();
        this.number = auto.getNumber();
    }
}
