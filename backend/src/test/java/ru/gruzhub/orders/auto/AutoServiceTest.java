package ru.gruzhub.orders.auto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.orders.auto.enums.AutoType;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.testing.UserTestingHelper;

@SpringBootTest
@ActiveProfiles("test")
public class AutoServiceTest {
    @Autowired
    private AutoService autoService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserTestingHelper userTestingHelper;

    private TestAutoServiceHelper testHelper;

    @BeforeEach
    public void setUp() {
        this.testHelper = new TestAutoServiceHelper(this.usersService, this.userTestingHelper);
    }

    @ParameterizedTest
    @EnumSource(AutoType.class)
    public void testCreateAuto(AutoType autoType) {
        Auto autoToCreate = this.testHelper.createAutoModel(autoType);
        Auto createdAuto = this.autoService.createAuto(autoToCreate);
        this.testHelper.validateModelsSame(autoToCreate, createdAuto);
    }

    @ParameterizedTest
    @EnumSource(AutoType.class)
    public void testUpdateAuto(AutoType autoType) {
        Auto auto = this.testHelper.createAutoModel(autoType);
        Auto createdAuto = this.autoService.createAuto(auto);

        Auto autoToUpdate = this.testHelper.createAutoModel(autoType);
        autoToUpdate.setId(createdAuto.getId());

        Auto updatedAuto = this.autoService.updateAuto(autoToUpdate);
        this.testHelper.validateModelsSame(autoToUpdate, updatedAuto);
    }

    @ParameterizedTest
    @EnumSource(AutoType.class)
    public void testGetSameAutoOnSameVinCreation(AutoType autoType) {
        Auto firstAuto = this.testHelper.createAutoModel(autoType);
        Auto createdFirstAuto = this.autoService.createAuto(firstAuto);

        Auto secondAuto = this.testHelper.createAutoModel(autoType);
        secondAuto.setVin(firstAuto.getVin());

        Auto createdSecondAuto = this.autoService.createAuto(secondAuto);

        assertEquals(createdFirstAuto.getId(), createdSecondAuto.getId());
        assertEquals(secondAuto.getModel(), createdSecondAuto.getModel());
    }

    @ParameterizedTest
    @EnumSource(AutoType.class)
    public void testGetSameAutoOnSameNumberCreation(AutoType autoType) {
        Auto firstAuto = this.testHelper.createAutoModel(autoType);
        Auto createdFirstAuto = this.autoService.createAuto(firstAuto);

        Auto secondAuto = this.testHelper.createAutoModel(autoType);
        secondAuto.setNumber(firstAuto.getNumber());

        Auto createdSecondAuto = this.autoService.createAuto(secondAuto);

        assertEquals(createdFirstAuto.getId(), createdSecondAuto.getId());
        assertEquals(secondAuto.getModel(), createdSecondAuto.getModel());
    }
}
