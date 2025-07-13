package ru.gruzhub.adresses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.address.service.AddressesService;
import ru.gruzhub.address.service.RegionsService;
import ru.gruzhub.address.models.Address;
import ru.gruzhub.address.models.Region;

@SpringBootTest
@ActiveProfiles("test")
class AddressesServiceTest {
    @Autowired
    private AddressesService addressesService;
    @Autowired
    private RegionsService regionsService;

    @Test
    void testCreateAddress() {
        Region region = this.regionsService.getRegionById(10L); // Assuming 1 exists
        Address address = new Address();
        address.setRegion(region);
        address.setCity("Test City");
        address.setStreet("Test Street");
        address.setLatitude(55.7558);
        address.setLongitude(37.6173);

        Address createdAddress = this.addressesService.createAddress(address);
        assertNotNull(createdAddress.getId());
        assertEquals("Test City", createdAddress.getCity());
    }

    @Test
    void testUpdateAddress() {
        // First, create an address
        Region region = this.regionsService.getRegionById(10L);
        Address address = new Address();
        address.setRegion(region);
        address.setCity("Test City");
        address.setStreet("Test Street");
        address.setLatitude(55.7558);
        address.setLongitude(37.6173);

        Address createdAddress = this.addressesService.createAddress(address);

        // Update the address
        createdAddress.setCity("Updated City");
        Address updatedAddress = this.addressesService.updateAddress(createdAddress);

        assertEquals("Updated City", updatedAddress.getCity());
    }

    @Test
    void testUpdateAddressWithoutId() {
        Address address = new Address();

        Exception exception = assertThrows(BadRequestException.class,
                                           () -> this.addressesService.updateAddress(address));

        String expectedMessage = "ID was not provided";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
