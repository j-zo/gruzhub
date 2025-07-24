package ru.gruzhub.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.transport.model.TransportColumn;

import java.util.UUID;

public interface TransportColumnRepository extends JpaRepository<TransportColumn, UUID> {
}
