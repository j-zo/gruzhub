package ru.gruzhub.address.service;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.address.models.Region;
import ru.gruzhub.address.repositories.AddressRepository;

@Service
@RequiredArgsConstructor
public class AddressesService {
    private final AddressRepository addressRepository;
    private final RegionsService regionsService;

    public Address createAddress(Address address) {
        Region region = regionsService.getRegionById(address.getRegion().getId());
        address.setRegion(region);
        return addressRepository.save(address);
    }

    public Address updateAddress(Address address) {
        if (address.getId() == null) {
            throw new BadRequestException("ID was not provided");
        }

        Address existingAddress = getAddressById(address.getId());
        Region region = regionsService.getRegionById(address.getRegion().getId());

        existingAddress.setCity(address.getCity());
        existingAddress.setStreet(address.getStreet());
        existingAddress.setLatitude(address.getLatitude());
        existingAddress.setLongitude(address.getLongitude());
        existingAddress.setRegion(region);

        return addressRepository.save(existingAddress);
    }

    public Address getAddressById(Long addressId) {
        return addressRepository.findById(addressId)
                                .orElseThrow(() -> new NotFoundException("Address with ID " +
                                                                         addressId +
                                                                         " not found"));
    }
}
