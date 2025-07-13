package ru.gruzhub.transport.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.transport.model.Transport;

public interface TransportRepository extends JpaRepository<Transport, Long> {
    Optional<Transport> findByVin(String vin);

    Optional<Transport> findByVinAndIdNot(String vin, Long idNot);

    Optional<Transport> findByNumber(String number);

    Optional<Transport> findByNumberAndIdNot(String number, Long idNot);
}
