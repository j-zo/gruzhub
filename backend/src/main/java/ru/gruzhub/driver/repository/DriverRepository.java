package ru.gruzhub.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.driver.model.Driver;

import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Driver create(Driver driver);
    Optional<Driver> findByNameOrEmailOrPhone(String name, String email, String phone);
}
