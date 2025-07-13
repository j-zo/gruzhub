package ru.gruzhub.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.transport.model.TransportColumn;

public interface TransportColumnRepository extends JpaRepository<TransportColumn, Long> {
}
