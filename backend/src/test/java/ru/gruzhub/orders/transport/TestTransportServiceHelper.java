package ru.gruzhub.orders.transport;

import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Service;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@Service
@RequiredArgsConstructor
public class TestTransportServiceHelper {
    private final UsersService usersService;
    private final UserTestingHelper userTestingHelper;
    private final Faker faker = new Faker();

    public TransportDto createTransportModel(TransportType transportType) {
        User customer = this.createUserByRole(UserRole.CUSTOMER);
        User driver = this.createUserByRole(UserRole.DRIVER);

        TransportDto transport = new TransportDto();
        transport.setCustomerId(customer.getId());
        transport.setDriverId(driver.getId());
        transport.setBrand(this.faker.company().name());
        transport.setModel(this.faker.company().name());
        transport.setVin(this.faker.regexify("[A-HJ-NPR-Z0-9]{17}"));
        transport.setNumber(this.faker.bothify("??####"));
        // TODO set valid value
        transport.setParkNumber(this.faker.bothify("??####"));
        transport.setType(transportType);

        return transport;
    }

    public void validateModelsSame(TransportDto model1, Transport model2) {
        Assertions.assertNotNull(model1.getCustomerId());
        Assertions.assertNotNull(model1.getDriverId());
        Assertions.assertNotNull(model2.getCustomer());
        Assertions.assertNotNull(model2.getDriver());

        Assertions.assertEquals(model1.getCustomerId(), model2.getCustomer().getId());
        Assertions.assertEquals(model1.getDriverId(), model2.getDriver().getId());
        Assertions.assertEquals(model1.getBrand(), model2.getBrand());
        Assertions.assertEquals(model1.getModel(), model2.getModel());
        Assertions.assertEquals(model1.getVin(), model2.getVin());
        Assertions.assertEquals(model1.getNumber(), model2.getNumber());
        Assertions.assertEquals(model1.getType(), model2.getType());
    }

    private User createUserByRole(UserRole role) {
        TestAuthDataDto authData = this.userTestingHelper.signUp(UserRole.CUSTOMER, null);
        this.userTestingHelper.updateRole(authData.getEmail(), role);
        return this.usersService.getUserById(authData.getUserId());
    }
}
