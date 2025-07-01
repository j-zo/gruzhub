package ru.gruzhub.orders.messages;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.gruzhub.orders.messages.dto.GetLastMessagePerOrderRequestDto;
import ru.gruzhub.orders.messages.dto.OrderMessageDto;
import ru.gruzhub.orders.messages.dto.SendMessageRequestDto;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.UserResponseDto;
import ru.gruzhub.users.models.User;

@RestController
@RequestMapping("/orders/messages")
@RequiredArgsConstructor
public class OrderMessagesController {
    private final OrderMessagesService messagesService;
    private final UsersService usersService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(
        @RequestBody SendMessageRequestDto sendMessageRequestDto) {
        User user = this.usersService.getCurrentUser();
        this.messagesService.sendMessage(user,
                                         sendMessageRequestDto.getGuaranteeId(),
                                         sendMessageRequestDto.getOrderId(),
                                         sendMessageRequestDto.getText(),
                                         null,
                                         null,
                                         null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-file")
    public ResponseEntity<Void> sendFileMessage(
        @RequestParam String guaranteeId,
        @RequestParam Long orderId,
        @RequestParam String filename,
        @RequestParam String extension,
        @RequestParam("file") MultipartFile file) {
        User user = this.usersService.getCurrentUser();

        try {
            byte[] fileBytes = file.getBytes();
            this.messagesService.sendMessage(user,
                                             guaranteeId,
                                             orderId,
                                             null,
                                             fileBytes,
                                             filename,
                                             extension);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send file message.", e);
        }
    }

    @PostMapping("/last-messages-per-order")
    public ResponseEntity<List<OrderMessageDto>> getLastMessagePerEachOrder(
        @RequestBody GetLastMessagePerOrderRequestDto request) {
        User user = this.usersService.getCurrentUser();

        List<OrderMessageDto> messages =
            this.messagesService.getLastMessagePerEachOrder(user, request.getOrdersIds());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/get-order-messages/{orderId}")
    public ResponseEntity<List<OrderMessageDto>> getOrderMessages(
        @PathVariable Long orderId) {
        User user = this.usersService.getCurrentUser();
        List<OrderMessageDto> messages = this.messagesService.getOrderMessages(user, orderId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/get-order-messages-users/{orderId}")
    public List<UserResponseDto> getOrderMessagesUsers(
        @PathVariable Long orderId) {
        User user = this.usersService.getCurrentUser();
        return this.messagesService.getOrderMessagesUsers(user, orderId);
    }

    @GetMapping("/set-messages-viewed-by-role/{orderId}")
    public void setMessagesViewedByRole(
        @PathVariable Long orderId) {
        User user = this.usersService.getCurrentUser();
        this.messagesService.setMessagesViewedByRole(user, orderId);
    }
}
