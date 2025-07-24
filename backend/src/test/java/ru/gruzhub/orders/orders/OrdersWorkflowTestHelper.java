package ru.gruzhub.orders.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.enums.TransportType;
import ru.gruzhub.orders.orders.dto.AuthWithOrderDto;
import ru.gruzhub.orders.orders.dto.CreateOrderRequestDto;
import ru.gruzhub.orders.orders.dto.CreateOrderResponseDto;
import ru.gruzhub.orders.orders.dto.DeclineOrderRequestDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.dto.OrderStatusChangeDto;
import ru.gruzhub.orders.orders.dto.OrderWithUsersDto;
import ru.gruzhub.users.UserRepository;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

public class OrdersWorkflowTestHelper {
    public static OrderWithUsersDto createOrderWithAttachedUsers(UsersService usersService,
                                                                 UserRepository userRepository,
                                                                 TestRestTemplate restTemplate,
                                                                 UserRole orderOwnerRole,
                                                                 @Nullable String masterToken,
                                                                 @Nullable Long regionId) {
        UserTestingHelper userTestingHelper = new UserTestingHelper(usersService, userRepository);

        if (regionId == null) {
            regionId = new Random().nextLong(20, 50);
        }

        if (masterToken == null) {
            TestAuthDataDto master = userTestingHelper.signUp(UserRole.MASTER, regionId);
            masterToken = "Bearer " + master.getAccessToken();
        }

        if (orderOwnerRole == UserRole.CUSTOMER) {
            TestAuthDataDto customer = userTestingHelper.signUp(UserRole.CUSTOMER, regionId);

            CreateOrderResponseDto order =
                createOrder(restTemplate, regionId, "Bearer " + customer.getAccessToken(), null, null);

            ResponseEntity<String> response =
                startCalculationByMaster(restTemplate, masterToken, order.getOrderId());
            assertEquals(HttpStatus.OK, response.getStatusCode());

            return new OrderWithUsersDto(order.getOrderId());
        }

        if (orderOwnerRole == UserRole.DRIVER) {
            CreateOrderResponseDto order = createOrder(restTemplate, regionId, null, null, null);

            ResponseEntity<String> response =
                startCalculationByMaster(restTemplate, masterToken, order.getOrderId());
            assertEquals(HttpStatus.OK, response.getStatusCode());

            return new OrderWithUsersDto(order.getOrderId());
        }

        throw new RuntimeException("Unsupported user role");
    }

    public static CreateOrderResponseDto createOrder(TestRestTemplate restTemplate,
                                                     String accessToken,
                                                     CreateOrderRequestDto orderRequest,
                                                     List<TransportDto> transportDtos) {
        return createOrder(restTemplate, null, accessToken, orderRequest, transportDtos);
    }

    public static CreateOrderResponseDto createOrder(TestRestTemplate restTemplate,
                                                     Long regionId,
                                                     String accessToken,
                                                     CreateOrderRequestDto orderRequest,
                                                     List<TransportDto> transportDtos) {
        if (orderRequest == null) {
            orderRequest = createOrderRequest(transportDtos, regionId);
        }

        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            headers.set("Authorization", accessToken);
        }

        ResponseEntity<CreateOrderResponseDto> responseEntity = restTemplate.postForEntity(
            "/orders/create",
            new HttpEntity<>(orderRequest, headers),
            CreateOrderResponseDto.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        return responseEntity.getBody();
    }

    public static CreateOrderRequestDto createOrderRequest() {
        return createOrderRequest(null);
    }

    public static CreateOrderRequestDto createOrderRequest(List<TransportDto> transportDtos) {
        return createOrderRequest(transportDtos, null);
    }

    public static CreateOrderRequestDto createOrderRequest(List<TransportDto> transportDtos,
                                                           Long regionId) {
        if (transportDtos == null) {
            transportDtos =
                Arrays.asList(createOrderTransport(TransportType.TRAILER), createOrderTransport(TransportType.TRUCK));
        }

        if (regionId == null) {
            regionId = new Random().nextLong(20, 50);
        }

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setGuaranteeUuid(UUID.randomUUID().toString());
        request.setDriverName("Driver " + UUID.randomUUID());
        request.setDriverPhone("Phone " + UUID.randomUUID());
        request.setDriverEmail("email" + UUID.randomUUID() + "@example.com");
        request.setCity("City " + UUID.randomUUID());
        request.setStreet("Street " + UUID.randomUUID());
        request.setTransport(transportDtos);
        request.setRegionId(regionId);
        request.setDescription("Description " + UUID.randomUUID());
        request.setNotes("Notes " + UUID.randomUUID());
        request.setNeedEvacuator(new Random().nextBoolean());
        request.setNeedMobileTeam(new Random().nextBoolean());
        request.setUrgency("Urgency " + UUID.randomUUID());
        return request;
    }

    public static TransportDto createOrderTransport(TransportType type) {
        TransportDto transportDto = new TransportDto();
        transportDto.setBrand("Brand " + UUID.randomUUID().toString().substring(0, 5));
        transportDto.setModel("Model " + UUID.randomUUID().toString().substring(0, 5));
        transportDto.setVin("VIN" + UUID.randomUUID().toString().substring(0, 8));
        transportDto.setNumber("Number " + UUID.randomUUID().toString().substring(0, 5));
        transportDto.setType(type);
        return transportDto;
    }

    public static OrderResponseDto getOrder(TestRestTemplate restTemplate,
                                            Long orderId) {
        HttpHeaders headers = new HttpHeaders();
        // TODO
      //  headers.set("Authorization", accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<OrderResponseDto> response = restTemplate.exchange("/orders/" + orderId,
                                                                          HttpMethod.GET,
                                                                          entity,
                                                                          OrderResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    public static ResponseEntity<String> startCalculationByMaster(TestRestTemplate restTemplate,
                                                                  String accessToken,
                                                                  Long orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        return restTemplate.exchange("/orders/" + orderId + "/start_calculation_by_master",
                                     HttpMethod.GET,
                                     requestEntity,
                                     String.class);
    }

    public static void declineOrderMaster(TestRestTemplate restTemplate,
                                          String accessToken,
                                          Long orderId,
                                          String comment) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        DeclineOrderRequestDto declineRequest = new DeclineOrderRequestDto();
        declineRequest.setComment(comment);
        HttpEntity<DeclineOrderRequestDto> requestEntity =
            new HttpEntity<>(declineRequest, headers);

        ResponseEntity<Void> response =
            restTemplate.postForEntity("/orders/" + orderId + "/decline_order_master",
                                       requestEntity,
                                       Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    public static void cancelOrder(TestRestTemplate restTemplate,
                                   String accessToken,
                                   Long orderId,
                                   String comment) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        DeclineOrderRequestDto cancelRequest = new DeclineOrderRequestDto();
        cancelRequest.setComment(comment);
        HttpEntity<DeclineOrderRequestDto> requestEntity = new HttpEntity<>(cancelRequest, headers);

        ResponseEntity<Void> response =
            restTemplate.postForEntity("/orders/" + orderId + "/cancel_order",
                                       requestEntity,
                                       Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    public static List<OrderStatusChangeDto> getOrderStatusChanges(TestRestTemplate restTemplate,
                                                                   Long orderId) {
        HttpHeaders headers = new HttpHeaders();
        //TODO adjust tests
        // headers.set("Authorization", accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<OrderStatusChangeDto[]> response = restTemplate.exchange(
            "/orders/order-status-changes/" + orderId,
            HttpMethod.GET,
            entity,
            OrderStatusChangeDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    public static AuthWithOrderDto createMasterWithOrderAndTakeIntoWork(TestRestTemplate restTemplate,
                                                                        UsersService usersService,
                                                                        UserRepository userRepository) {
        Long regionId = new Random().nextLong(20, 50);

        UserTestingHelper userTestingHelper = new UserTestingHelper(usersService, userRepository);
        TestAuthDataDto masterAuthData = userTestingHelper.signUp(UserRole.MASTER, regionId);

        CreateOrderResponseDto createdOrder =
            createOrder(restTemplate, regionId, masterAuthData.getAccessToken(), null, null);

        startCalculationByMaster(restTemplate,
                                 masterAuthData.getAccessToken(),
                                 createdOrder.getOrderId());

        OrderResponseDto order =
            getOrder(restTemplate, createdOrder.getOrderId());

        return new AuthWithOrderDto(masterAuthData, order);
    }
}
