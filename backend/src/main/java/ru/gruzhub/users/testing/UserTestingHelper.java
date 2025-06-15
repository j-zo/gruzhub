package ru.gruzhub.users.testing;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import lombok.AllArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import ru.gruzhub.users.UserRepository;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.CreateUserRequestDto;
import ru.gruzhub.users.dto.SignInUserRequestDto;
import ru.gruzhub.users.dto.SignInUserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@Component
@AllArgsConstructor
public class UserTestingHelper {
    private final UsersService usersService;
    private final UserRepository userRepository;

    public void updateRole(String email, UserRole role) {
        Optional<User> optionalUser = this.userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setRole(role);
            this.userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public TestAuthDataDto signUp(UserRole role) {
        return this.signUp(role, null);
    }

    public TestAuthDataDto signUp(UserRole role, Long regionId) {
        Faker faker = new Faker();
        String email = Instant.now().toEpochMilli() +
                       UUID.randomUUID().toString() +
                       faker.internet().emailAddress();
        String password = faker.internet().password();

        if (regionId == null) {
            regionId = new Random().nextLong(10, 80);
        }

        UserRole registeringRole = role;
        if (registeringRole == UserRole.ADMIN) {
            registeringRole = UserRole.CUSTOMER;
        }

        CreateUserRequestDto createUserRequest = new CreateUserRequestDto();
        createUserRequest.setName(faker.name().fullName());
        createUserRequest.setEmail(email);
        createUserRequest.setPassword(password);
        createUserRequest.setRole(registeringRole);
        createUserRequest.setPhone(faker.phoneNumber().phoneNumber());
        createUserRequest.setCity(faker.address().city());
        createUserRequest.setStreet(faker.address().streetAddress());
        createUserRequest.setRegionId(regionId);
        this.usersService.signUp(createUserRequest);

        if (role == UserRole.ADMIN) {
            this.updateRole(email, UserRole.ADMIN);
        }

        SignInUserRequestDto signInRequest = new SignInUserRequestDto();
        signInRequest.setEmail(email);
        signInRequest.setPassword(password);
        signInRequest.setRole(registeringRole);
        SignInUserResponseDto signInResponse = this.usersService.signIn(signInRequest);

        return new TestAuthDataDto(signInResponse.getId(), signInResponse.getAccessToken(), email);
    }
}
