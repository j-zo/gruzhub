package ru.gruzhub.orders.auto;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.orders.auto.models.Auto;

public interface AutoRepository extends JpaRepository<Auto, Long> {
    Optional<Auto> findByVin(String vin);

    Optional<Auto> findByVinAndIdNot(String vin, Long idNot);

    Optional<Auto> findByNumber(String number);

    Optional<Auto> findByNumberAndIdNot(String number, Long idNot);
}
