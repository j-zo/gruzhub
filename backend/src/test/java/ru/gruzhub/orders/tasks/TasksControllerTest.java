package ru.gruzhub.orders.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.transport.TransportRepository;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.orders.orders.OrdersWorkflowTestHelper;
import ru.gruzhub.orders.orders.dto.AuthWithOrderDto;
import ru.gruzhub.orders.orders.dto.OrderResponseDto;
import ru.gruzhub.orders.orders.repository.OrderRepository;
import ru.gruzhub.orders.tasks.dto.CreateTaskDto;
import ru.gruzhub.orders.tasks.dto.TaskResponseDto;
import ru.gruzhub.orders.tasks.dto.UpdateTaskDto;
import ru.gruzhub.users.UserRepository;
import ru.gruzhub.users.UsersService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TasksControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UsersService usersService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TransportRepository transportRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Test
    void testCreateTask() {
        AuthWithOrderDto authWithOrder =
            OrdersWorkflowTestHelper.createMasterWithOrderAndTakeIntoWork(this.restTemplate,
                                                                          this.usersService,
                                                                          this.userRepository);

        // Create task
        CreateTaskDto createTask =
            TasksTestHelper.createTaskSchema(authWithOrder.getOrder().getId(),
                                             authWithOrder.getOrder()
                                                          .getTransports()
                                                          .getFirst()
                                                          .getId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authWithOrder.getAuthData().getAccessToken());
        HttpEntity<CreateTaskDto> request = new HttpEntity<>(createTask, headers);

        // Act
        ResponseEntity<TaskResponseDto> response =
            this.restTemplate.postForEntity("/tasks/create", request, TaskResponseDto.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TaskResponseDto createdTask = response.getBody();
        assertNotNull(createdTask);
        assertNotNull(createdTask.getId());
        assertEquals(authWithOrder.getOrder().getId(), createdTask.getOrderId());
        assertEquals(authWithOrder.getOrder().getTransports().getFirst().getId(),
                     createdTask.getTransportId());
        assertEquals(createTask.getName(), createdTask.getName());
        assertEquals(createTask.getDescription(), createdTask.getDescription());
        assertNotNull(createdTask.getPrice());
        assertEquals(createTask.getPrice(), createdTask.getPrice());
    }

    @Test
    void testGetOrderTransportTasks() {
        AuthWithOrderDto authWithOrder =
            OrdersWorkflowTestHelper.createMasterWithOrderAndTakeIntoWork(this.restTemplate,
                                                                          this.usersService,
                                                                          this.userRepository);

        OrderResponseDto order = authWithOrder.getOrder();
        TransportDto transport = authWithOrder.getOrder().getTransports().getFirst();

        // Create two tasks
        CreateTaskDto task1 = TasksTestHelper.createTaskSchema(order.getId(), transport.getId());
        CreateTaskDto task2 = TasksTestHelper.createTaskSchema(order.getId(), transport.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authWithOrder.getAuthData().getAccessToken());

        ResponseEntity<TaskResponseDto> response1 = this.restTemplate.postForEntity("/tasks/create",
                                                                                    new HttpEntity<>(
                                                                                        task1,
                                                                                        headers),
                                                                                    TaskResponseDto.class);
        assertEquals(response1.getStatusCode(), HttpStatus.OK);

        ResponseEntity<TaskResponseDto> response2 = this.restTemplate.postForEntity("/tasks/create",
                                                                                    new HttpEntity<>(
                                                                                        task2,
                                                                                        headers),
                                                                                    TaskResponseDto.class);
        assertEquals(response2.getStatusCode(), HttpStatus.OK);

        // Act: Retrieve tasks
        ResponseEntity<TaskResponseDto[]> response =
            this.restTemplate.exchange("/tasks/order_transport_tasks?orderId=" +
                                       order.getId() +
                                       "&transportId=" +
                                       transport.getId(),
                                       HttpMethod.GET,
                                       new HttpEntity<>(headers),
                                       TaskResponseDto[].class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TaskResponseDto[] tasks = response.getBody();
        assertNotNull(tasks);
        assertEquals(2, tasks.length);
    }

    @Test
    void testUpdateTask() {
        AuthWithOrderDto authWithOrder =
            OrdersWorkflowTestHelper.createMasterWithOrderAndTakeIntoWork(this.restTemplate,
                                                                          this.usersService,
                                                                          this.userRepository);

        OrderResponseDto order = authWithOrder.getOrder();
        TransportDto transport = authWithOrder.getOrder().getTransports().getFirst();

        CreateTaskDto createTask = TasksTestHelper.createTaskSchema(order.getId(), transport.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authWithOrder.getAuthData().getAccessToken());

        ResponseEntity<TaskResponseDto> createResponse = this.restTemplate.postForEntity(
            "/tasks/create",
            new HttpEntity<>(createTask, headers),
            TaskResponseDto.class);

        TaskResponseDto createdTask = createResponse.getBody();
        assertNotNull(createdTask);

        // Prepare update
        UpdateTaskDto updateTask = TasksTestHelper.createUpdateTaskDto(createdTask.getId(),
                                                                       "Updated Task Name",
                                                                       "Updated Task Description",
                                                                       "150.00");

        HttpEntity<UpdateTaskDto> updateRequest = new HttpEntity<>(updateTask, headers);

        // Act: Update task
        ResponseEntity<Void> updateResponse =
            this.restTemplate.postForEntity("/tasks/update", updateRequest, Void.class);

        // Assert update response
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());

        // Retrieve updated task
        ResponseEntity<TaskResponseDto[]> getResponse =
            this.restTemplate.exchange("/tasks/order_transport_tasks?orderId=" +
                                       order.getId() +
                                       "&transportId=" +
                                       transport.getId(),
                                       HttpMethod.GET,
                                       new HttpEntity<>(headers),
                                       TaskResponseDto[].class);

        TaskResponseDto[] tasks = getResponse.getBody();
        assertNotNull(tasks);
        assertTrue(tasks.length > 0);

        TaskResponseDto updatedTask = tasks[0];
        assertEquals(updateTask.getName(), updatedTask.getName());
        assertEquals(updateTask.getDescription(), updatedTask.getDescription());
        assertNotNull(updatedTask.getPrice());
        assertEquals(updateTask.getPrice(), updatedTask.getPrice());
    }

    @Test
    void testDeleteTask() {
        AuthWithOrderDto authWithOrderDto =
            OrdersWorkflowTestHelper.createMasterWithOrderAndTakeIntoWork(this.restTemplate,
                                                                          this.usersService,
                                                                          this.userRepository);

        OrderResponseDto order = authWithOrderDto.getOrder();
        TransportDto transport = order.getTransports().getFirst();

        CreateTaskDto createTask = TasksTestHelper.createTaskSchema(order.getId(), transport.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authWithOrderDto.getAuthData().getAccessToken());

        ResponseEntity<TaskResponseDto> createResponse = this.restTemplate.postForEntity(
            "/tasks/create",
            new HttpEntity<>(createTask, headers),
            TaskResponseDto.class);

        TaskResponseDto createdTask = createResponse.getBody();
        assertNotNull(createdTask);

        // Act: Delete task
        ResponseEntity<Void> deleteResponse =
            this.restTemplate.exchange("/tasks/delete/" + createdTask.getId(),
                                       HttpMethod.DELETE,
                                       new HttpEntity<>(headers),
                                       Void.class);

        // Assert delete response
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        // Retrieve tasks to ensure deletion
        ResponseEntity<TaskResponseDto[]> getResponse =
            this.restTemplate.exchange("/tasks/order_transport_tasks?orderId=" +
                                       order.getId() +
                                       "&transportId=" +
                                       transport.getId(),
                                       HttpMethod.GET,
                                       new HttpEntity<>(headers),
                                       TaskResponseDto[].class);

        TaskResponseDto[] tasks = getResponse.getBody();
        assertNotNull(tasks);
        assertEquals(0, tasks.length);
    }
}
