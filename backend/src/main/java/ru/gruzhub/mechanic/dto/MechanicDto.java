package ru.gruzhub.mechanic.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.mechanic.model.Mechanic;
import ru.gruzhub.transport.dto.TransportColumnDto;
import ru.gruzhub.transport.model.TransportColumn;

@Getter
@Setter
@NoArgsConstructor
public class MechanicDto {

    private TransportColumnDto transportColumn;
    private String name;
    private String phone;
    private String email;

    public MechanicDto(Mechanic mechanic) {
        this.transportColumn = new TransportColumnDto(mechanic.getTransportColumn());

        this.name = mechanic.getName();
        this.phone = mechanic.getPhone();
        this.email = mechanic.getEmail();
    }
}
