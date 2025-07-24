package ru.gruzhub.orders.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.gruzhub.orders.messages.dto.OrderMessageDto;
import ru.gruzhub.orders.messages.dto.SendMessageRequestDto;
import ru.gruzhub.users.dto.UserDto;

public class OrderMessagesControllerHelper {
    public static SendMessageRequestDto sendMessage(TestRestTemplate restTemplate,
                                                    Long orderId) {
        SendMessageRequestDto body = new SendMessageRequestDto();
        body.setGuaranteeId(UUID.randomUUID() + "-" + System.currentTimeMillis());
        body.setOrderId(orderId);
        body.setText("Sample message text");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SendMessageRequestDto> request = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response =
            restTemplate.postForEntity("/orders/messages/send", request, Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        return body;
    }

    public static List<OrderMessageDto> getOrderMessages(TestRestTemplate restTemplate,
                                                         Long orderId) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<OrderMessageDto[]> response = restTemplate.exchange(
            "/orders/messages/get-order-messages/" + orderId,
            HttpMethod.GET,
            request,
            OrderMessageDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    public static List<OrderMessageDto> getLastMessagePerEachOrder(TestRestTemplate restTemplate,
                                                                   List<Long> ordersIds,
                                                                   String accessToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("ordersIds", ordersIds);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<OrderMessageDto[]> response = restTemplate.postForEntity(
            "/orders/messages/last-messages-per-order",
            request,
            OrderMessageDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    public static List<UserDto> getOrderMessagesUsers(TestRestTemplate restTemplate,
                                                      Long orderId,
                                                      String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<UserDto[]> response = restTemplate.exchange(
            "/orders/messages/get-order-messages-users/" + orderId,
            HttpMethod.GET,
            request,
            UserDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }


    public static void setMessagesViewedByRole(TestRestTemplate restTemplate,
                                               Long orderId) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/orders/messages/set-messages-viewed-by-role/" + orderId,
            HttpMethod.GET,
            request,
            Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}