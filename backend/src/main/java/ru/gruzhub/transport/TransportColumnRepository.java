package ru.gruzhub.transport;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.transport.model.TransportColumn;

import java.util.Optional;
import java.util.UUID;

public interface TransportColumnRepository extends JpaRepository<TransportColumn, UUID> {
    Optional<TransportColumn> findByColumnNumber(String number);
}
