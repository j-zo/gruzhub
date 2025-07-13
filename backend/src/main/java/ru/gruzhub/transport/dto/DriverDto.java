package ru.gruzhub.transport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.driver.model.Driver;

@Getter
@Setter
@NoArgsConstructor
public class DriverDto {

    private Long id;
    private String name;
    private String phone;
    private String email;

    public DriverDto(Driver driver) {
        this.id = driver.getId();
        this.name = driver.getName();
        this.phone = driver.getPhone();
        this.email = driver.getEmail();
    }
}
