package ru.gruzhub.orders.auto;

import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Service;
import ru.gruzhub.orders.auto.enums.AutoType;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@Service
@RequiredArgsConstructor
public class TestAutoServiceHelper {
    private final UsersService usersService;
    private final UserTestingHelper userTestingHelper;
    private final Faker faker = new Faker();

    public Auto createAutoModel(AutoType autoType) {
        User customer = this.createUserByRole(UserRole.CUSTOMER);
        User driver = this.createUserByRole(UserRole.DRIVER);

        Auto auto = new Auto();
        auto.setCustomer(customer);
        auto.setDriver(driver);
        auto.setBrand(this.faker.company().name());
        auto.setModel(this.faker.company().name());
        auto.setVin(this.faker.regexify("[A-HJ-NPR-Z0-9]{17}"));
        auto.setNumber(this.faker.bothify("??####"));
        auto.setType(autoType);
        auto.setMerged(false);
        auto.setMergedTo(null);

        return auto;
    }

    public void validateModelsSame(Auto model1, Auto model2) {
        Assertions.assertNotNull(model1.getCustomer());
        Assertions.assertNotNull(model1.getDriver());
        Assertions.assertNotNull(model2.getCustomer());
        Assertions.assertNotNull(model2.getDriver());

        Assertions.assertEquals(model1.getCustomer().getId(), model2.getCustomer().getId());
        Assertions.assertEquals(model1.getDriver().getId(), model2.getDriver().getId());
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
