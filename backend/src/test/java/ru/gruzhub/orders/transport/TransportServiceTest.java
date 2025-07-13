package ru.gruzhub.orders.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.service.TransportService;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.testing.UserTestingHelper;

@SpringBootTest
@ActiveProfiles("test")
public class TransportServiceTest {
    @Autowired
    private TransportService transportService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UserTestingHelper userTestingHelper;

    private TestTransportServiceHelper testHelper;

    @BeforeEach
    public void setUp() {
        this.testHelper = new TestTransportServiceHelper(this.usersService, this.userTestingHelper);
    }

    @ParameterizedTest
    @EnumSource(TransportType.class)
    public void testCreateTransport(TransportType transportType) {
        TransportDto transportToCreate = this.testHelper.createTransportModel(transportType);
        Transport createdTransport = this.transportService.createTransport(transportToCreate);
        this.testHelper.validateModelsSame(transportToCreate, createdTransport);
    }

    @ParameterizedTest
    @EnumSource(TransportType.class)
    public void testUpdateTransport(TransportType transportType) {
        TransportDto transport = this.testHelper.createTransportModel(transportType);
        Transport createdTransport = this.transportService.createTransport(transport);

        TransportDto transportToUpdate = this.testHelper.createTransportModel(transportType);
        transportToUpdate.setId(createdTransport.getId());

        Transport updatedAuto = this.transportService.updateTransport(createdTransport, transportToUpdate);
        this.testHelper.validateModelsSame(transportToUpdate, updatedAuto);
    }

    @ParameterizedTest
    @EnumSource(TransportType.class)
    public void testGetSameAutoOnSameVinCreation(TransportType autoType) {
        TransportDto firstTransport = this.testHelper.createTransportModel(autoType);
        Transport createdFirstTransport = this.transportService.createTransport(firstTransport);

        TransportDto secondTransport = this.testHelper.createTransportModel(autoType);
        secondTransport.setVin(firstTransport.getVin());

        Transport createdSecondTransport = this.transportService.createTransport(secondTransport);

        assertEquals(createdFirstTransport.getId(), createdSecondTransport.getId());
        assertEquals(secondTransport.getModel(), createdSecondTransport.getModel());
    }

    @ParameterizedTest
    @EnumSource(TransportType.class)
    public void testGetSameAutoOnSameNumberCreation(TransportType autoType) {
        TransportDto firstTransport = this.testHelper.createTransportModel(autoType);
        Transport createdFirstTransport = this.transportService.createTransport(firstTransport);

        TransportDto secondTransport = this.testHelper.createTransportModel(autoType);
        secondTransport.setNumber(firstTransport.getNumber());

        Transport createdSecondTransport = this.transportService.createTransport(secondTransport);

        assertEquals(createdFirstTransport.getId(), createdSecondTransport.getId());
        assertEquals(secondTransport.getModel(), createdSecondTransport.getModel());
    }
}
