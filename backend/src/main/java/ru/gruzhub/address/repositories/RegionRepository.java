package ru.gruzhub.address.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.address.models.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByName(String name);
}
