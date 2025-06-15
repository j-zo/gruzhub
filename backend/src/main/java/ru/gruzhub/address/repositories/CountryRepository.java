package ru.gruzhub.address.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.address.models.Country;

public interface CountryRepository extends JpaRepository<Country, String> {
  Optional<Country> findByName(String name);
}
