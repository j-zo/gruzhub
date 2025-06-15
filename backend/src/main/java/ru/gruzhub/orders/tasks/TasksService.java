package ru.gruzhub.orders.tasks;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.orders.auto.AutoService;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.services.OrdersDataService;
import ru.gruzhub.orders.tasks.dto.CreateTaskDto;
import ru.gruzhub.orders.tasks.dto.TaskResponseDto;
import ru.gruzhub.orders.tasks.dto.UpdateTaskDto;
import ru.gruzhub.orders.tasks.models.Task;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class TasksService {
    private final AutoService autoService;
    private final TaskRepository taskRepository;
    private final OrdersDataService ordersDataService;

    public TaskResponseDto createTask(User user, CreateTaskDto createRequest) {
        Order order = this.ordersDataService.getOrderById(user, createRequest.getOrderId());
        Auto auto = this.autoService.getAutoById(createRequest.getAutoId());

        assert order.getMaster() != null;
        if (!Objects.equals(order.getMaster().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "You are not the master of this order.");
        }

        List<Long> orderAutosIds = order.getAutos().stream().map(Auto::getId).toList();
        if (!orderAutosIds.contains(auto.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "Auto does not belong to the order.");
        }

        Task task = Task.builder()
                        .auto(auto)
                        .order(order)
                        .name(createRequest.getName())
                        .description(createRequest.getDescription())
                        .price(createRequest.getPrice() != null ? createRequest.getPrice() : null)
                        .createdAt(System.currentTimeMillis())
                        .updatedAt(System.currentTimeMillis())
                        .build();
        this.taskRepository.save(task);

        return task.toDto();
    }

    public void updateTask(User user, UpdateTaskDto updateRequest) {
        Task task = this.getTaskById(updateRequest.getId());

        assert task.getOrder().getMaster() != null;
        if (!Objects.equals(task.getOrder().getMaster().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "You are not the master of this order.");
        }

        task.setName(updateRequest.getName());
        task.setDescription(updateRequest.getDescription());
        task.setPrice(updateRequest.getPrice() != null ? updateRequest.getPrice() : null);
        task.setUpdatedAt(System.currentTimeMillis());

        this.taskRepository.save(task);
    }

    public void deleteTask(User user, Long taskId) {
        Task task = this.getTaskById(taskId);

        assert task.getOrder().getMaster() != null;
        if (!Objects.equals(task.getOrder().getMaster().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "You are not the master of this order.");
        }

        this.taskRepository.delete(task);
    }

    public List<TaskResponseDto> getOrderAutoTasks(User authorizedUser, Long orderId, Long autoId) {
        if (orderId == null && autoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "Either orderId or autoId should be specified");
        }

        Order order = this.ordersDataService.getOrderById(authorizedUser, orderId);

        boolean isUserPresentInOrder = false;

        if (order.getDriver() != null &&
            Objects.equals(order.getDriver().getId(), authorizedUser.getId())) {
            isUserPresentInOrder = true;
        } else if (order.getMaster() != null &&
                   Objects.equals(order.getMaster().getId(), authorizedUser.getId())) {
            isUserPresentInOrder = true;
        } else if (order.getCustomer() != null &&
                   Objects.equals(order.getCustomer().getId(), authorizedUser.getId())) {
            isUserPresentInOrder = true;
        }

        if (authorizedUser.getRole() != UserRole.ADMIN && !isUserPresentInOrder) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        List<Task> tasks;

        if (autoId != null) {
            tasks = this.taskRepository.findByOrderIdAndAutoId(orderId,
                                                               autoId,
                                                               Sort.by(Sort.Direction.DESC, "id"));
        } else {
            tasks = this.taskRepository.findByOrderId(orderId, Sort.by(Sort.Direction.DESC, "id"));
        }

        return tasks.stream().map(Task::toDto).toList();
    }

    public Task getTaskById(Long taskId) {
        return this.taskRepository.findById(taskId).orElseThrow();
    }
}
