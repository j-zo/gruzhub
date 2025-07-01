package ru.gruzhub.users;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gruzhub.users.dto.CreateUserRequestDto;
import ru.gruzhub.users.dto.GetUsersRequestDto;
import ru.gruzhub.users.dto.SignInUserRequestDto;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.dto.UpdateUserRequestDto;
import ru.gruzhub.users.dto.UserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {
    private final UsersService usersService;

    @PostMapping("/signup")
    public void signUp(@RequestBody CreateUserRequestDto userRequest) {
        this.usersService.signUp(userRequest);
    }

    @PostMapping("/signin")
    public SignInUserResponseDto signIn(@RequestBody SignInUserRequestDto signInRequest) {
        return this.usersService.signIn(signInRequest);
    }

    @PostMapping("/update")
    public void update(@RequestBody UpdateUserRequestDto updateRequest) {
        this.usersService.updateRequest(updateRequest);
    }

    @GetMapping("/reset-code")
    public void sendResetCode(@RequestParam String email, @RequestParam String role) {
        this.usersService.sendResetCode(email, UserRole.valueOf(role));
    }

    @GetMapping("/reset-password")
    public void resetPassword(@RequestParam String email,
                              @RequestParam String role,
                              @RequestParam String code,
                              @RequestParam String password) {
        this.usersService.resetPassword(email, code, password, UserRole.valueOf(role));
    }

    @GetMapping("/{userId}")
    public UserResponseDto getUser(@PathVariable Long userId) {
        return this.usersService.getUserByIdWithAuth(userId);
    }

    @GetMapping("/get-access/{userId}")
    public ResponseEntity<SignInUserResponseDto> getUserAccess(@PathVariable Long userId,
                                                               @RequestHeader(HttpHeaders.AUTHORIZATION)
                                                               String authorization) {
        SignInUserResponseDto response = this.usersService.getUserAccess(authorization, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connect-telegram-via-webapp")
    public void connectTelegramViaWebapp(
        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestParam Long tgId) {
        this.usersService.connectTelegramViaWebApp(authorization, tgId);

    }

    @PostMapping("/users")
    public List<UserResponseDto> getUsers(@RequestBody GetUsersRequestDto getUsersRequest,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION)
                                          String authorization) {
        return this.usersService.getUsers(authorization, getUsersRequest);
    }

    @GetMapping("/connect-chat")
    public void connectChat(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                            @RequestParam String chatUuid) {
        User user = this.usersService.getUserFromToken(authorization);
        this.usersService.connectTelegramChat(user, chatUuid);
    }

    @GetMapping("/disconnect-chat")
    public void disconnectChat(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                               @RequestParam String chatUuid) {
        User user = this.usersService.getUserFromToken(authorization);
        this.usersService.disconnectTelegramChat(user, chatUuid);
    }
}
