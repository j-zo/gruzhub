package ru.gruzhub.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@SpringBootTest
public class UsersServiceTest {
    @Autowired
    private UsersService usersService;
    @Autowired
    private UserRepository userRepository;

    @Test
    public void testIncreaseBalance() {
        // Create a user with role MASTER
        UserTestingHelper userTestingHelper =
            new UserTestingHelper(this.usersService, this.userRepository);
        TestAuthDataDto authDto = userTestingHelper.signUp(UserRole.MASTER, null);

        Long userId = authDto.getUserId();

        // Fetch the user to get initial balance
        User user = this.userRepository.findById(userId).orElse(null);
        assertNotNull(user, "User should not be null");

        BigDecimal initialBalance = user.getBalance();

        BigDecimal increaseAmount = new BigDecimal("500.50");

        // Increase the user's balance
        this.usersService.increaseUserBalance(userId, increaseAmount);

        // Fetch the user again to check balance
        User updatedUser = this.userRepository.findById(userId).orElse(null);
        assertNotNull(updatedUser, "Updated user should not be null");

        BigDecimal expectedBalance = initialBalance.add(increaseAmount);

        assertEquals(0,
                     updatedUser.getBalance().compareTo(expectedBalance),
                     "Balance should be increased correctly");
    }

    @Test
    public void testDecreaseBalance() {
        // Create a user with role MASTER
        UserTestingHelper userTestingHelper =
            new UserTestingHelper(this.usersService, this.userRepository);
        TestAuthDataDto authDto = userTestingHelper.signUp(UserRole.MASTER, null);
        Long userId = authDto.getUserId();

        // Fetch the user to get initial balance
        User user = this.userRepository.findById(userId).orElse(null);
        assertNotNull(user, "User should not be null");

        BigDecimal initialBalance = user.getBalance();

        BigDecimal increaseAmount = new BigDecimal("500.50");

        // Increase the user's balance
        this.usersService.increaseUserBalance(userId, increaseAmount);

        // Now decrease the user's balance
        BigDecimal decreaseAmount = new BigDecimal("125.33");

        this.usersService.decreaseUserBalance(userId, decreaseAmount);

        // Fetch the user again to check balance
        User updatedUser = this.userRepository.findById(userId).orElse(null);
        assertNotNull(updatedUser, "Updated user should not be null");

        BigDecimal expectedBalance = initialBalance.add(increaseAmount).subtract(decreaseAmount);

        assertEquals(0,
                     updatedUser.getBalance().compareTo(expectedBalance),
                     "Balance should be decreased correctly");
    }

    @Test
    public void testCannotDecreaseNotEnoughBalance() {
        // Create a user with role MASTER
        UserTestingHelper userTestingHelper =
            new UserTestingHelper(this.usersService, this.userRepository);
        TestAuthDataDto authDto = userTestingHelper.signUp(UserRole.MASTER, null);
        Long userId = authDto.getUserId();

        // Fetch the user to get initial balance
        User user = this.userRepository.findById(userId).orElse(null);
        assertNotNull(user, "User should not be null");

        BigDecimal initialBalance = user.getBalance();

        BigDecimal increaseAmount = new BigDecimal("100.50");

        // Increase the user's balance
        this.usersService.increaseUserBalance(userId, increaseAmount);

        // Attempt to decrease more than the balance
        BigDecimal decreaseAmount = initialBalance.add(new BigDecimal("125.33"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            this.usersService.decreaseUserBalance(userId, decreaseAmount);
        });

        String expectedMessage = "На балансе не хватает средств";
        String actualMessage = exception.getReason();

        assertEquals(expectedMessage, actualMessage, "Exception message should match");
    }
}
