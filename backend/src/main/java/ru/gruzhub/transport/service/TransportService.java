package ru.gruzhub.transport.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.document.model.Document;
import ru.gruzhub.document.repository.DocumentRepository;
import ru.gruzhub.driver.model.Driver;
import ru.gruzhub.driver.repository.DriverRepository;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.transport.model.TransportColumn;
import ru.gruzhub.transport.repository.TransportColumnRepository;
import ru.gruzhub.transport.repository.TransportRepository;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class TransportService {
    private final UsersService usersService;
    private final TransportRepository transportRepository;
    private final TransportColumnRepository transportColumnRepository;
    private final DriverRepository driverRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public Transport createTransport(TransportDto transport) {
        // Check for existing VIN
        if (transport.getVin() != null) {
            Optional<Transport> sameVinTransport = this.transportRepository.findByVin(transport.getVin());
            if (sameVinTransport.isPresent()) {
                return this.updateTransport(sameVinTransport.get(), transport);
            }
        }

        // Check for existing Number
        if (transport.getNumber() != null) {
            Optional<Transport> sameNumberTransport = this.transportRepository.findByNumber(transport.getNumber());
            if (sameNumberTransport.isPresent()) {
                return this.updateTransport(sameNumberTransport.get(), transport);
            }
        }

        // TODO check for existing park number

        Transport newTransport = Transport.builder()
                .brand(transport.getBrand())
                .isMerged(false)
                .model(transport.getModel())
                .number(transport.getNumber())
                .parkNumber(transport.getParkNumber())
                .type(transport.getType())
                .vin(transport.getVin())
                .build();

        Optional<User> customer = usersService.getUserById(transport.getCustomerId());
        newTransport.setCustomer(customer.orElseGet(usersService::getCurrentUser));

        driverRepository.findById(transport.getDriverId()).ifPresent(newTransport::setDriver);
        transportColumnRepository.findById(transport.getTransportColumnId()).ifPresent(newTransport::setTransportColumn);
        transportRepository.findById(transport.getMainTransportId()).ifPresent(newTransport::setMainTransport);
        if (transport.getDocumentIds() != null) {
            newTransport.setDocuments(transport.getDocumentIds()
                    .stream()
                    .map(documentRepository::findById)
                    .filter(Optional::isPresent).map(Optional::get)
                    .toList());
        }
        return this.transportRepository.save(newTransport);
    }

    public Transport updateTransport(Transport transport, TransportDto updatedValues) {
        Transport existingTransport = this.getTransportById(transport.getId());
        Transport originalTransport =
                this.getOriginalTransportByVinOrNumber(transport.getId(), transport.getVin(), transport.getNumber());

        if (originalTransport != null) {
            existingTransport.setMerged(true);
            existingTransport.setMergedTo(originalTransport);

            this.mergeDuplicatedTransportFieldsToOriginalTransport(existingTransport, originalTransport);

            existingTransport = originalTransport;
        }

        existingTransport.setBrand(updatedValues.getBrand());
        existingTransport.setNumber(updatedValues.getNumber());
        existingTransport.setModel(updatedValues.getModel());
        existingTransport.setType(updatedValues.getType());
        existingTransport.setVin(updatedValues.getVin());

        if (existingTransport.getMainTransport() == null && updatedValues.getMainTransportId() != null
                || existingTransport.getMainTransport() != null && !Objects.equals(existingTransport.getMainTransport().getId(), updatedValues.getMainTransportId())) {
            Optional<Transport> mainTransport = this.transportRepository.findById(updatedValues.getMainTransportId());
            mainTransport.ifPresent(existingTransport::setMainTransport);
        }

        if (existingTransport.getDriver() == null && updatedValues.getDriverId() != null
                || existingTransport.getDriver() != null && !Objects.equals(existingTransport.getDriver().getId(), updatedValues.getDriverId())) {
            Optional<Driver> driver = this.driverRepository.findById(updatedValues.getDriverId());
            driver.ifPresent(existingTransport::setDriver);
        }

        if (existingTransport.getCustomer() == null && updatedValues.getCustomerId() != null
                || existingTransport.getCustomer() != null && !Objects.equals(existingTransport.getCustomer().getId(), updatedValues.getCustomerId())) {
            Optional<User> customer = usersService.getUserById(updatedValues.getCustomerId());
            customer.ifPresent(existingTransport::setCustomer);
        }

        if (existingTransport.getTransportColumn() == null && updatedValues.getTransportColumnId() != null
                || existingTransport.getTransportColumn() != null && !Objects.equals(existingTransport.getTransportColumn().getId(), updatedValues.getTransportColumnId())) {
            Optional<TransportColumn> column = this.transportColumnRepository.findById(updatedValues.getTransportColumnId());
            column.ifPresent(existingTransport::setTransportColumn);
        }

        if (existingTransport.getDocuments().isEmpty() && updatedValues.getDocumentIds() != null && !updatedValues.getDocumentIds().isEmpty()) {
            existingTransport.getDocuments().addAll(updatedValues
                    .getDocumentIds()
                    .stream()
                    .map(documentRepository::findById)
                    .map(doc -> doc.orElse(null))
                    .filter(Objects::nonNull)
                    .toList());
        }

        HashSet<Long> idsToRemove = new HashSet<>();
        idsToRemove.addAll(updatedValues.getDocumentIds());
        idsToRemove.addAll(existingTransport.getDocuments().stream().map(Document::getId).toList());

        updatedValues.getDocumentIds().forEach(idsToRemove::remove);
        idsToRemove.forEach(this.documentRepository::removeById);
        return transportRepository.save(existingTransport);
    }

    public Transport getTransportById(Long transportId) {
        return this.transportRepository.findById(transportId)
                                  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                 "Transport not found"));
    }

    public TransportDto getTransportByIdWithAuth(String authorization, Long transportId) {
        User user = this.usersService.getUserFromToken(authorization);
        Transport transport = this.getTransportById(transportId);

        if ((transport.getDriver() != null && !transport.getDriver().getId().equals(user.getId())) &&
            (transport.getCustomer() != null && !transport.getCustomer().getId().equals(user.getId())) &&
            user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return new TransportDto(transport);
    }

    private Transport getOriginalTransportByVinOrNumber(Long excludeId, String vin, String number) {
        if (vin != null) {
            Optional<Transport> transportByVin = this.transportRepository.findByVinAndIdNot(vin, excludeId);
            if (transportByVin.isPresent()) {
                return transportByVin.get();
            }
        }

        if (number != null) {
            Optional<Transport> transportByNumber =
                this.transportRepository.findByNumberAndIdNot(number, excludeId);
            if (transportByNumber.isPresent()) {
                return transportByNumber.get();
            }
        }

        return null;
    }

    private void mergeDuplicatedTransportFieldsToOriginalTransport(Transport newTransport, Transport originalTransport) {
        if (newTransport.getDriver() != null && originalTransport.getDriver() == null) {
            originalTransport.setDriver(newTransport.getDriver());
        }
        if (newTransport.getCustomer() != null && originalTransport.getCustomer() == null) {
            originalTransport.setCustomer(newTransport.getCustomer());
        }
    }
}
