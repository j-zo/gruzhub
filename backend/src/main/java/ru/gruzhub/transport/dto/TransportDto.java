package ru.gruzhub.transport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.gruzhub.document.model.Document;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.transport.model.Transport;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class TransportDto {
    private Long id;
    private TransportType type;
    // TODO: what is this customer field???
    private Long customerId;
    // TODO: add main transport dto?
    private Long mainTransportId;
    // TODO: Add transport column dto?
    private Long transportColumnId;
    private List<Long> documentIds;
    // TODO: Add driver dto???
    private Long driverId;
    private String brand;
    private String model;
    private String vin;
    private String number;
    private String parkNumber;

    public TransportDto(Transport transport) {
        this.id = transport.getId();
        this.type = transport.getType();

        if (transport.getCustomer() != null) {
            this.customerId = transport.getCustomer().getId();
        }

        if (transport.getDriver() != null) {
            this.driverId = transport.getDriver().getId();
        }

        if (transport.getMainTransport() != null) {
            this.mainTransportId = transport.getMainTransport().getId();
        }

        if (transport.getTransportColumn() != null) {
            this.transportColumnId = transport.getTransportColumn().getId();
        }

        if (transport.getDocuments() != null) {
            this.documentIds = transport.getDocuments().stream().map(Document::getId).collect(Collectors.toList());
        }
        
        this.brand = transport.getBrand();
        this.model = transport.getModel();
        this.vin = transport.getVin();
        this.number = transport.getNumber();
        this.parkNumber = transport.getParkNumber();
    }
}
