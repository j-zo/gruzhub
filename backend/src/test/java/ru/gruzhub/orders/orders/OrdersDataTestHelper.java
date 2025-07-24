package ru.gruzhub.orders.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.orders.orders.dto.GetOrdersRequestDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.enums.OrderStatus;

public class OrdersDataTestHelper {
    public static TransportDto getOrderTransport(TestRestTemplate restTemplate,
                                                 String accessToken,
                                                 Long orderId,
                                                 UUID transportId) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<TransportDto> response =
            restTemplate.exchange("/orders/transport?orderId=" + orderId + "&transportId=" + transportId,
                                  HttpMethod.GET,
                                  entity,
                                  TransportDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    public static List<OrderResponseDto> getOrders(TestRestTemplate restTemplate,
                                                   List<OrderStatus> statuses) {

        HttpHeaders headers = new HttpHeaders();
        // TODO
        //headers.set("Authorization", accessToken);

        GetOrdersRequestDto requestDto = new GetOrdersRequestDto();
        requestDto.setStatuses(statuses);

        HttpEntity<GetOrdersRequestDto> entity = new HttpEntity<>(requestDto, headers);

        ResponseEntity<OrderResponseDto[]> response = restTemplate.exchange("/orders/orders",
                                                                            HttpMethod.POST,
                                                                            entity,
                                                                            OrderResponseDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }
}
