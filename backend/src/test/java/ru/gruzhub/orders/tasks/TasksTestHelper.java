package ru.gruzhub.orders.tasks;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.gruzhub.orders.tasks.dto.CreateTaskDto;
import ru.gruzhub.orders.tasks.dto.TaskResponseDto;
import ru.gruzhub.orders.tasks.dto.UpdateTaskDto;

public class TasksTestHelper {
    public static CreateTaskDto createTaskSchema(Long orderId, Long transportId) {
        String name = "Task " + UUID.randomUUID().toString().substring(0, 8);
        String description = "Description " + UUID.randomUUID().toString().substring(0, 12);
        String price = String.format("%d.%02d",
                                     ThreadLocalRandom.current().nextInt(1, 101),
                                     ThreadLocalRandom.current().nextInt(0, 100));
        return CreateTaskDto.builder()
                            .orderId(orderId)
                            .transportId(transportId)
                            .name(name)
                            .description(description)
                            .price(new BigDecimal(price))
                            .build();
    }

    public static UpdateTaskDto createUpdateTaskDto(Long taskId,
                                                    String newName,
                                                    String newDescription,
                                                    String newPrice) {
        return UpdateTaskDto.builder()
                            .id(taskId)
                            .name(newName)
                            .description(newDescription)
                            .price(new BigDecimal(newPrice))
                            .build();
    }

    public static List<TaskResponseDto> getOrderTransportTasks(TestRestTemplate restTemplate,
                                                          Long orderId,
                                                          Long transportId,
                                                          String accessToken) {
        String url = String.format("/tasks/order_transport_tasks?orderId=%d&transportId=%d", orderId, transportId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<TaskResponseDto[]> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, TaskResponseDto[].class);

        assert response.getStatusCode() == HttpStatus.OK;
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    public static TaskResponseDto createTask(TestRestTemplate restTemplate,
                                             CreateTaskDto taskSchema,
                                             String accessToken) {
        String url = "/tasks/create";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        HttpEntity<CreateTaskDto> entity = new HttpEntity<>(taskSchema, headers);

        ResponseEntity<TaskResponseDto> response =
            restTemplate.postForEntity(url, entity, TaskResponseDto.class);

        assert response.getStatusCode() == HttpStatus.OK;
        return response.getBody();
    }
}
