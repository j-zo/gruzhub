package ru.gruzhub.transport.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.transport.model.Transport;

public interface TransportRepository extends JpaRepository<Transport, UUID> {
    Optional<Transport> findByVin(String vin);

    Optional<Transport> findByVinAndIdNot(String vin, UUID idNot);

    Optional<Transport> findByNumber(String number);

    Optional<Transport> findByNumberAndIdNot(String number, UUID idNot);
}
