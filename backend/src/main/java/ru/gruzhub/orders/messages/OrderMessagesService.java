package ru.gruzhub.orders.messages;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.orders.messages.dto.OrderMessageDto;
import ru.gruzhub.orders.messages.models.OrderMessage;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.service.OrdersDataService;
import ru.gruzhub.tools.files.FilesService;
import ru.gruzhub.tools.files.models.File;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.UserDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class OrderMessagesService {
    private final UsersService usersService;
    private final OrdersDataService ordersDataService;
    private final OrderMessagesRepository messagesRepository;
    private final FilesService filesService;

    public void sendMessage(User user,
                            String guaranteeId,
                            Long orderId,
                            String text,
                            byte[] fileBytes,
                            String filename,
                            String extension) {
        Order order = this.ordersDataService.getOrderById(user, orderId);

        this.validateMessagesAuthority(user, order);

        if (this.messagesRepository.findByOrderIdAndGuaranteeId(orderId, guaranteeId).isPresent()) {
            return;
        }

        OrderMessage message = new OrderMessage();
        message.setGuaranteeId(guaranteeId);
        message.setOrder(order);
        message.setUser(user);
        message.setUserRole(user.getRole());

        if (text != null && !text.isEmpty()) {
            final int MAX_TEXT_SIZE = 10_000;
            if (text.length() > MAX_TEXT_SIZE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слишком много текста");
            }

            message.setText(text);
        } else if (fileBytes != null && filename != null && extension != null) {
            File fileModel = this.filesService.createFile(user, fileBytes, filename, extension);
            message.setFile(fileModel);
            message.setFileCode(fileModel.getCode());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              "Text or file must be provided with the message.");
        }

        message.setDate(System.currentTimeMillis());
        message.setViewedByCustomer(user.getRole() == UserRole.CUSTOMER);
        message.setViewedByDriver(user.getRole() == UserRole.DRIVER);
        message.setViewedByMaster(user.getRole() == UserRole.MASTER);

        this.messagesRepository.save(message);
    }

    public List<OrderMessageDto> getLastMessagePerEachOrder(User user, List<Long> ordersIds) {
        List<OrderMessage> lastMessages =
            this.messagesRepository.findLastMessagesPerOrder(user.getId(), ordersIds);
        lastMessages.sort(Comparator.comparing(OrderMessage::getDate).reversed());
        return lastMessages.stream().map(OrderMessageDto::new).collect(Collectors.toList());
    }

    public List<OrderMessageDto> getOrderMessages(User user, Long orderId) {
        Order order = this.ordersDataService.getOrderById(user, orderId);

        this.validateMessagesAuthority(user, order);

        List<OrderMessage> messages = this.messagesRepository.findOrderMessages(orderId);
        return messages.stream().map(OrderMessageDto::new).collect(Collectors.toList());
    }

    public List<UserDto> getOrderMessagesUsers(User user, Long orderId) {
        Order order = this.ordersDataService.getOrderById(user, orderId);

        this.validateMessagesAuthority(user, order);

        List<Long> usersIds = this.messagesRepository.findOrderMessagesUserIds(orderId);
        return this.usersService.getUsersByIds(usersIds);
    }

    public void setMessagesViewedByRole(User user, Long orderId) {
        Order order = this.ordersDataService.getOrderById(user, orderId);

        this.validateMessagesAuthority(user, order);

        UserRole role = user.getRole();
        List<OrderMessage> messages = this.messagesRepository.findOrderMessages(orderId);

        for (OrderMessage message : messages) {
            if (role == UserRole.CUSTOMER && !message.isViewedByCustomer()) {
                message.setViewedByCustomer(true);
            } else if (role == UserRole.MASTER && !message.isViewedByMaster()) {
                message.setViewedByMaster(true);
            } else if (role == UserRole.DRIVER && !message.isViewedByDriver()) {
                message.setViewedByDriver(true);
            }
        }

        this.messagesRepository.saveAll(messages);
    }

    private void validateMessagesAuthority(User user, Order order) {
        boolean isUserInOrder = false;

        if (order.getMaster() != null && user.getId().equals(order.getMaster().getId())) {
            isUserInOrder = true;
        } else if (order.getDriver() != null && user.getId().equals(order.getDriver().getId())) {
            isUserInOrder = true;
        } else if (order.getCustomer() != null &&
                   user.getId().equals(order.getCustomer().getId())) {
            isUserInOrder = true;
        }

        if (user.getRole() != UserRole.ADMIN && !isUserInOrder) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                              "You do not have permission to access these " +
                                              "messages.");
        }
    }
}
