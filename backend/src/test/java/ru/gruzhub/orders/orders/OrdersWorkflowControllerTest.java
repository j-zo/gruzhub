package ru.gruzhub.orders.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.dto.OrderTransportDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrdersWorkflowControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UsersService usersService;
    @Autowired
    private UserTestingHelper userTestingHelper;

    @Test
    void testCreateAnonymousOrder() {
        CreateOrderResponseDto createResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, null, null);
        assertNotNull(createResponse.getAccessToken());
        assertNotNull(createResponse.getDriverId());
        assertNotNull(createResponse.getOrderId());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"DRIVER", "CUSTOMER"})
    void testCreateOrder(UserRole userRole) {
        TestAuthDataDto authData = this.userTestingHelper.signUp(UserRole.CUSTOMER);
        CreateOrderResponseDto response = OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                                               authData.getAccessToken(),
                                                                               null,
                                                                               null);
        this.userTestingHelper.updateRole(authData.getEmail(), userRole);

        assertNotNull(response.getOrderId());
    }

    @Test
    void testCreateOrderForExistingTransport() {
        List<OrderTransportDto> transports =
            Collections.singletonList(OrdersWorkflowTestHelper.createOrderTransport(TransportType.TRAILER));

        CreateOrderResponseDto firstOrderResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, null, transports);
        CreateOrderResponseDto secondOrderResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, null, transports);

        assertNotNull(firstOrderResponse.getAccessToken());
        assertNotNull(secondOrderResponse.getAccessToken());

        OrderResponseDto firstOrder = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                        firstOrderResponse.getOrderId(),
                                                                        firstOrderResponse.getAccessToken());
        OrderResponseDto secondOrder = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                         secondOrderResponse.getOrderId(),
                                                                         secondOrderResponse.getAccessToken());

        assertEquals(firstOrder.getTransports().getFirst().getId(),
                     secondOrder.getTransports().getFirst().getId());
    }

    @Test
    void testGetOrder() {
        CreateOrderRequestDto orderToCreate = OrdersWorkflowTestHelper.createOrderRequest();

        CreateOrderResponseDto createResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, orderToCreate, null);
        assertNotNull(createResponse.getAccessToken());

        OrderResponseDto orderResponse = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                           createResponse.getOrderId(),
                                                                           createResponse.getAccessToken());

        assertEquals(orderToCreate.getDescription(), orderResponse.getDescription());
        assertEquals(orderToCreate.isNeedEvacuator(), orderResponse.isNeedEvacuator());
        assertEquals(orderToCreate.isNeedMobileTeam(), orderResponse.isNeedMobileTeam());
        assertEquals(OrderStatus.CREATED, orderResponse.getStatus());
        assertNotNull(orderResponse.getDriverId());
        assertNotNull(orderResponse.getCreatedAt());
        assertNotNull(orderResponse.getUpdatedAt());
        assertEquals(orderToCreate.getRegionId(), orderResponse.getAddress().getRegion().getId());
        assertEquals(orderToCreate.getCity(), orderResponse.getAddress().getCity());
        assertEquals(orderToCreate.getStreet(), orderResponse.getAddress().getStreet());
        assertEquals(orderToCreate.getUrgency(), orderResponse.getUrgency());

        int EXPECTED_ORDERS_AUTO_COUNT = 2;
        assertEquals(EXPECTED_ORDERS_TRANSPORT_COUNT, orderResponse.getTransports().size());

        // Sort and compare autos
        orderResponse.getTransports().sort(Comparator.comparing(TransportDto::getBrand));
        orderToCreate.getTransport().sort(Comparator.comparing(OrderTransportDto::getBrand));

        for (int i = 0; i < EXPECTED_ORDERS_AUTO_COUNT; i++) {
            TransportDto autoResponse = orderResponse.getTransports().get(i);
            OrderTransportDto autoRequest = orderToCreate.getTransport().get(i);

            assertEquals(autoRequest.getBrand(), autoResponse.getBrand());
            assertEquals(autoRequest.getModel(), autoResponse.getModel());
            assertEquals(autoRequest.getVin(), autoResponse.getVin());
            assertEquals(autoRequest.getNumber(), autoResponse.getNumber());
            assertEquals(autoRequest.getType(), autoResponse.getType());
        }
    }

    @Test
    void testGetSameOrderOnSameGuaranteeUuid() {
        String guaranteeUuid = UUID.randomUUID().toString();
        CreateOrderRequestDto firstRequest = OrdersWorkflowTestHelper.createOrderRequest();
        firstRequest.setGuaranteeUuid(guaranteeUuid);

        CreateOrderRequestDto secondRequest = OrdersWorkflowTestHelper.createOrderRequest();
        secondRequest.setGuaranteeUuid(guaranteeUuid);

        CreateOrderResponseDto firstOrderResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, firstRequest, null);
        CreateOrderResponseDto secondOrderResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, secondRequest, null);

        assertEquals(firstOrderResponse.getOrderId(), secondOrderResponse.getOrderId());
    }

    @Test
    void testMasterCannotStartAnotherRegionOrderCalculation() {
        TestAuthDataDto masterAuthData = this.userTestingHelper.signUp(UserRole.MASTER);
        CreateOrderResponseDto createdOrder =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, null, null);

        // Start calculation by master
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", masterAuthData.getAccessToken());
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = this.restTemplate.exchange("/orders/" +
                                                                   createdOrder.getOrderId() +
                                                                   "/start_calculation_by_master",
                                                                   HttpMethod.GET,
                                                                   requestEntity,
                                                                   Void.class);

        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testMasterCannotStartAlreadyStartedOrder() {
        long regionId = new Random().nextLong(20, 50);

        TestAuthDataDto master1AuthData = this.userTestingHelper.signUp(UserRole.MASTER, regionId);
        TestAuthDataDto master2AuthData = this.userTestingHelper.signUp(UserRole.MASTER, regionId);

        CreateOrderResponseDto order =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, regionId, null, null, null);

        // Master 1 starts calculation
        ResponseEntity<String> response1 =
            OrdersWorkflowTestHelper.startCalculationByMaster(this.restTemplate,
                                                              master1AuthData.getAccessToken(),
                                                              order.getOrderId());
        assertEquals(HttpStatus.OK, response1.getStatusCode());

        // Master 2 tries to start calculation
        ResponseEntity<String> response2 =
            OrdersWorkflowTestHelper.startCalculationByMaster(this.restTemplate,
                                                              master2AuthData.getAccessToken(),
                                                              order.getOrderId());
        assertNotEquals(HttpStatus.OK, response2.getStatusCode());
    }

    @Test
    void testMasterCannotGetForeignMasterOrder() {
        long regionId = new Random().nextLong(20, 50);

        TestAuthDataDto master1AuthData = this.userTestingHelper.signUp(UserRole.MASTER, regionId);
        TestAuthDataDto master2AuthData = this.userTestingHelper.signUp(UserRole.MASTER, regionId);

        CreateOrderResponseDto order =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, null, null);

        // Master 1 starts calculation
        OrdersWorkflowTestHelper.startCalculationByMaster(this.restTemplate,
                                                          master1AuthData.getAccessToken(),
                                                          order.getOrderId());

        // Master 2 tries to get the order
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", master2AuthData.getAccessToken());
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
            this.restTemplate.exchange("/orders/" + order.getOrderId(),
                                       HttpMethod.GET,
                                       requestEntity,
                                       String.class);

        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeclineOrderMasterWithRefund() {
        long regionId = new Random().nextLong(20, 50);

        TestAuthDataDto customerAuth = this.userTestingHelper.signUp(UserRole.CUSTOMER, regionId);
        TestAuthDataDto masterAuth = this.userTestingHelper.signUp(UserRole.MASTER, regionId);

        CreateOrderResponseDto createdOrder =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                 regionId,
                                                 customerAuth.getAccessToken(),
                                                 null,
                                                 null);

        BigDecimal masterInitialBalance =
            this.usersService.getUserById(masterAuth.getUserId()).getBalance();

        // Master starts calculation
        ResponseEntity<String> response =
            OrdersWorkflowTestHelper.startCalculationByMaster(this.restTemplate,
                                                              masterAuth.getAccessToken(),
                                                              createdOrder.getOrderId());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Customer declines order
        OrdersWorkflowTestHelper.declineOrderMaster(this.restTemplate,
                                                    customerAuth.getAccessToken(),
                                                    createdOrder.getOrderId(),
                                                    UUID.randomUUID().toString());

        // Fetch order and verify status
        OrderResponseDto order = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                   createdOrder.getOrderId(),
                                                                   customerAuth.getAccessToken());
        assertNull(order.getMaster());
        assertNull(order.getMasterId());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        BigDecimal masterBalanceAfter =
            this.usersService.getUserById(masterAuth.getUserId()).getBalance();
        assertEquals(masterInitialBalance, masterBalanceAfter);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"CUSTOMER", "ADMIN"})
    void testCancelOrderWithRefund(UserRole userRole) {
        long regionId = new Random().nextLong(20, 50);

        TestAuthDataDto customerAuth = this.userTestingHelper.signUp(UserRole.CUSTOMER, regionId);
        TestAuthDataDto masterAuth = this.userTestingHelper.signUp(UserRole.MASTER, regionId);
        TestAuthDataDto adminAuth = null;

        if (userRole == UserRole.ADMIN) {
            adminAuth = this.userTestingHelper.signUp(UserRole.ADMIN);
        }

        CreateOrderResponseDto createdOrder =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                 customerAuth.getAccessToken(),
                                                 null,
                                                 null);

        BigDecimal masterInitialBalance =
            this.usersService.getUserById(masterAuth.getUserId()).getBalance();

        // Master starts calculation
        OrdersWorkflowTestHelper.startCalculationByMaster(this.restTemplate,
                                                          masterAuth.getAccessToken(),
                                                          createdOrder.getOrderId());

        // Cancel order
        String accessToken = (userRole == UserRole.CUSTOMER) ?
                             customerAuth.getAccessToken() :
                             adminAuth.getAccessToken();
        OrdersWorkflowTestHelper.cancelOrder(this.restTemplate,
                                             accessToken,
                                             createdOrder.getOrderId(),
                                             UUID.randomUUID().toString());

        // Fetch order and verify status
        OrderResponseDto order = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                   createdOrder.getOrderId(),
                                                                   customerAuth.getAccessToken());
        assertNull(order.getMaster());
        assertNull(order.getMasterId());
        assertEquals(OrderStatus.CANCEL, order.getStatus());

        // Verify master's balance is refunded
        BigDecimal masterBalanceAfter =
            this.usersService.getUserById(masterAuth.getUserId()).getBalance();
        assertEquals(masterInitialBalance, masterBalanceAfter);
    }
}
