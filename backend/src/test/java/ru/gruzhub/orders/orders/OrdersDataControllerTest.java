package ru.gruzhub.orders.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import ru.gruzhub.orders.auto.dto.AutoResponseDto;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.dto.OrderStatusChangeDto;
import ru.gruzhub.orders.orders.dto.UpdateOrderAutoRequestDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.services.OrdersWorkflowService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrdersDataControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserTestingHelper userTestingHelper;
    @Autowired
    private OrdersWorkflowService ordersWorkflowService;

    @Test
    void testGetDriverOrders() {
        // Prepare schemas
        CreateOrderRequestDto orderToCreate1BySameDriver =
            OrdersWorkflowTestHelper.createOrderRequest();
        CreateOrderRequestDto orderToCreate2BySameDriver =
            OrdersWorkflowTestHelper.createOrderRequest();
        orderToCreate2BySameDriver.setDriverPhone(orderToCreate1BySameDriver.getDriverPhone());

        CreateOrderRequestDto orderToCreateByDifferentDriver =
            OrdersWorkflowTestHelper.createOrderRequest();

        // Create orders
        CreateOrderResponseDto order1BySameDriver =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                 null,
                                                 orderToCreate1BySameDriver,
                                                 null);
        CreateOrderResponseDto order2BySameDriver =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                 null,
                                                 orderToCreate2BySameDriver,
                                                 null);
        OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                             null,
                                             orderToCreateByDifferentDriver,
                                             null);

        // Get orders
        assertNotNull(order1BySameDriver.getAccessToken());
        List<OrderResponseDto> orders = OrdersDataTestHelper.getOrders(this.restTemplate,
                                                                       order1BySameDriver.getAccessToken(),
                                                                       null);

        // Check orders
        int EXPECTED_ORDERS_COUNT = 2;
        assertEquals(EXPECTED_ORDERS_COUNT, orders.size());
        assertEquals(order2BySameDriver.getDriverId(), orders.get(0).getDriverId());
        assertEquals(order1BySameDriver.getDriverId(), orders.get(1).getDriverId());
    }

    @Test
    void testGetOrderStatusChanges() {
        CreateOrderResponseDto orderResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, null, null);
        assertNotNull(orderResponse.getAccessToken());

        List<OrderStatusChangeDto> statusChanges = OrdersWorkflowTestHelper.getOrderStatusChanges(
            this.restTemplate,
            orderResponse.getOrderId(),
            orderResponse.getAccessToken());

        assertEquals(1, statusChanges.size());
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void testGetCustomerOrders(OrderStatus orderStatus) {
        TestAuthDataDto customerAuthData = this.userTestingHelper.signUp(UserRole.CUSTOMER);

        List<CreateOrderResponseDto> createdOrders = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            CreateOrderResponseDto createdOrderResponse =
                OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                     customerAuthData.getAccessToken(),
                                                     null,
                                                     null);

            this.ordersWorkflowService.updateOrderStatusForTesting(createdOrderResponse.getOrderId(),
                                                                   orderStatus);

            createdOrders.add(createdOrderResponse);
        }

        List<OrderResponseDto> orders = OrdersDataTestHelper.getOrders(this.restTemplate,
                                                                       customerAuthData.getAccessToken(),
                                                                       Collections.singletonList(
                                                                           orderStatus));

        int EXPECTED_ORDERS_COUNT = 2;
        assertEquals(EXPECTED_ORDERS_COUNT, orders.size());
        assertEquals(createdOrders.get(1).getOrderId(), orders.get(0).getId());
        assertEquals(createdOrders.get(0).getOrderId(), orders.get(1).getId());

        for (OrderResponseDto order : orders) {
            assertEquals(orderStatus, order.getStatus());
        }
    }

    @Test
    void testMasterGetsNewOrdersInRegion() {
        Long regionId = new Random().nextLong(20, 50);

        TestAuthDataDto masterAuthData = this.userTestingHelper.signUp(UserRole.MASTER, regionId);
        CreateOrderResponseDto createdOrder =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, regionId, null, null, null);

        List<OrderResponseDto> orders = OrdersDataTestHelper.getOrders(this.restTemplate,
                                                                       masterAuthData.getAccessToken(),
                                                                       null);

        List<Long> ordersIds = new ArrayList<>();
        for (OrderResponseDto order : orders) {
            ordersIds.add(order.getId());
        }

        assertTrue(ordersIds.contains(createdOrder.getOrderId()));
    }

    @Test
    void testGetAutoOrders() {
        CreateOrderRequestDto orderToCreate = OrdersWorkflowTestHelper.createOrderRequest();
        CreateOrderResponseDto createResponse =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate, null, orderToCreate, null);
        assertNotNull(createResponse.getAccessToken());

        OrderResponseDto orderResponse = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                           createResponse.getOrderId(),
                                                                           createResponse.getAccessToken());
        Long autoId = orderResponse.getAutos().getFirst().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createResponse.getAccessToken());
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<OrderResponseDto[]> response =
            this.restTemplate.exchange("/orders/auto/" + autoId,
                                       HttpMethod.GET,
                                       entity,
                                       OrderResponseDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).length);
    }

    @Test
    void testGetOrderAuto() {
        TestAuthDataDto authData = this.userTestingHelper.signUp(UserRole.CUSTOMER);
        CreateOrderResponseDto createdOrder =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                 authData.getAccessToken(),
                                                 null,
                                                 null);

        OrderResponseDto order = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                   createdOrder.getOrderId(),
                                                                   authData.getAccessToken());
        AutoResponseDto createdOrderAuto = order.getAutos().getFirst();

        AutoResponseDto auto = OrdersDataTestHelper.getOrderAuto(this.restTemplate,
                                                                 authData.getAccessToken(),
                                                                 createdOrder.getOrderId(),
                                                                 createdOrderAuto.getId());

        assertEquals(createdOrderAuto.getBrand(), auto.getBrand());
        assertEquals(createdOrderAuto.getModel(), auto.getModel());
        assertEquals(createdOrderAuto.getVin(), auto.getVin());
        assertEquals(createdOrderAuto.getNumber(), auto.getNumber());
    }

    @Test
    void testUpdateOrderAuto() {
        TestAuthDataDto authData = this.userTestingHelper.signUp(UserRole.CUSTOMER);
        CreateOrderResponseDto createdOrder =
            OrdersWorkflowTestHelper.createOrder(this.restTemplate,
                                                 authData.getAccessToken(),
                                                 null,
                                                 null);

        OrderResponseDto order = OrdersWorkflowTestHelper.getOrder(this.restTemplate,
                                                                   createdOrder.getOrderId(),
                                                                   authData.getAccessToken());
        AutoResponseDto createdOrderAuto = order.getAutos().getFirst();

        UpdateOrderAutoRequestDto autoToUpdate = new UpdateOrderAutoRequestDto();
        autoToUpdate.setOrderId(order.getId());
        autoToUpdate.setAutoId(createdOrderAuto.getId());
        autoToUpdate.setBrand(UUID.randomUUID().toString());
        autoToUpdate.setModel(UUID.randomUUID().toString());
        autoToUpdate.setVin(UUID.randomUUID().toString());
        autoToUpdate.setNumber(UUID.randomUUID().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authData.getAccessToken());
        HttpEntity<UpdateOrderAutoRequestDto> requestEntity =
            new HttpEntity<>(autoToUpdate, headers);

        ResponseEntity<Void> response =
            this.restTemplate.postForEntity("/orders/auto", requestEntity, Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        AutoResponseDto updatedAuto = OrdersDataTestHelper.getOrderAuto(this.restTemplate,
                                                                        authData.getAccessToken(),
                                                                        createdOrder.getOrderId(),
                                                                        createdOrderAuto.getId());

        assertEquals(autoToUpdate.getBrand(), updatedAuto.getBrand());
        assertEquals(autoToUpdate.getModel(), updatedAuto.getModel());
        assertEquals(autoToUpdate.getVin(), updatedAuto.getVin());
        assertEquals(autoToUpdate.getNumber(), updatedAuto.getNumber());
    }
}
