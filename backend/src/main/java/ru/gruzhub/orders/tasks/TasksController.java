package ru.gruzhub.orders.tasks;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.orders.tasks.dto.CreateTaskDto;
import ru.gruzhub.orders.tasks.dto.TaskResponseDto;
import ru.gruzhub.orders.tasks.dto.UpdateTaskDto;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.models.User;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TasksController {
    private final TasksService tasksService;
    private final UsersService usersService;

    @PostMapping("/create")
    public TaskResponseDto createTask(
        @RequestBody CreateTaskDto createTask) {
        User user = this.usersService.getCurrentUser();
        return this.tasksService.createTask(user, createTask);
    }

    @PostMapping("/update")
    public void updateTask(@RequestBody UpdateTaskDto updateTask) {
        User user = this.usersService.getCurrentUser();
        this.tasksService.updateTask(user, updateTask);
    }

    @DeleteMapping("/delete/{taskId}")
    public void deleteTask(@PathVariable("taskId") Long taskId) {
        User user = this.usersService.getCurrentUser();
        this.tasksService.deleteTask(user, taskId);
    }

    @GetMapping("/order_auto_tasks")
    public List<TaskResponseDto> getOrderAutoTasks(
        @RequestParam(required = false) Long orderId,
        @RequestParam(required = false) Long autoId) {
        User user = this.usersService.getCurrentUser();
        return this.tasksService.getOrderAutoTasks(user, orderId, autoId);
    }
}
