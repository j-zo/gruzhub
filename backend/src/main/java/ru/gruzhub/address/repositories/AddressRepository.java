package ru.gruzhub.address.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.address.models.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {}
