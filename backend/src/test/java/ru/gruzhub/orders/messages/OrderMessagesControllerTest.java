package ru.gruzhub.orders.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.gruzhub.orders.messages.dto.OrderMessageDto;
import ru.gruzhub.orders.messages.dto.SendMessageRequestDto;
import ru.gruzhub.orders.orders.OrdersWorkflowTestHelper;
import ru.gruzhub.orders.orders.dto.OrderWithUsersDto;
import ru.gruzhub.users.UserRepository;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.dto.UserResponseDto;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.testing.UserTestingHelper;
import ru.gruzhub.users.testing.dto.TestAuthDataDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrderMessagesControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UsersService usersService;
    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"DRIVER", "CUSTOMER"})
    public void testSendMessage(UserRole orderOwnerRole) {
        OrderWithUsersDto orderWithUsers = OrdersWorkflowTestHelper.createOrderWithAttachedUsers(
            this.usersService,
            this.userRepository,
            this.restTemplate,
            orderOwnerRole,
            null,
            null);

        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  orderWithUsers.getOrderId(),
                                                  orderWithUsers.getOrderOwnerToken());

        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  orderWithUsers.getOrderId(),
                                                  orderWithUsers.getMasterToken());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"DRIVER", "CUSTOMER"})
    public void testGetLastMessagesPerEachOrder(UserRole orderOwnerRole) {
        long regionId = new Random().nextLong(20, 50);

        UserTestingHelper userTestingHelper =
            new UserTestingHelper(this.usersService, this.userRepository);
        TestAuthDataDto master = userTestingHelper.signUp(UserRole.MASTER, regionId);

        OrderWithUsersDto order1WithUsers = OrdersWorkflowTestHelper.createOrderWithAttachedUsers(
            this.usersService,
            this.userRepository,
            this.restTemplate,
            orderOwnerRole,
            master.getAccessToken(),
            regionId);
        OrderWithUsersDto order2WithUsers = OrdersWorkflowTestHelper.createOrderWithAttachedUsers(
            this.usersService,
            this.userRepository,
            this.restTemplate,
            orderOwnerRole,
            master.getAccessToken(),
            regionId);

        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  order1WithUsers.getOrderId(),
                                                  order1WithUsers.getOrderOwnerToken());
        SendMessageRequestDto lastMessageOfOrder1 =
            OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                      order1WithUsers.getOrderId(),
                                                      order1WithUsers.getMasterToken());

        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  order2WithUsers.getOrderId(),
                                                  order2WithUsers.getOrderOwnerToken());
        SendMessageRequestDto lastMessageOfOrder2 =
            OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                      order2WithUsers.getOrderId(),
                                                      order2WithUsers.getMasterToken());

        List<OrderMessageDto> lastMessagesPerEachOrder =
            OrderMessagesControllerHelper.getLastMessagePerEachOrder(this.restTemplate,
                                                                     Arrays.asList(order1WithUsers.getOrderId(),
                                                                                   order2WithUsers.getOrderId()),
                                                                     master.getAccessToken());

        assertEquals(2, lastMessagesPerEachOrder.size());

        assertEquals(lastMessageOfOrder1.getText(), lastMessagesPerEachOrder.get(0).getText());
        assertEquals(lastMessageOfOrder2.getText(), lastMessagesPerEachOrder.get(1).getText());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"DRIVER", "CUSTOMER"})
    public void testGetOrderMessages(UserRole orderOwnerRole) {
        OrderWithUsersDto orderWithUsers = OrdersWorkflowTestHelper.createOrderWithAttachedUsers(
            this.usersService,
            this.userRepository,
            this.restTemplate,
            orderOwnerRole,
            null,
            null);

        SendMessageRequestDto lastMasterMessageRequestBody =
            OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                      orderWithUsers.getOrderId(),
                                                      orderWithUsers.getMasterToken());
        SendMessageRequestDto lastOwnerMessageRequestBody =
            OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                      orderWithUsers.getOrderId(),
                                                      orderWithUsers.getOrderOwnerToken());

        for (int i = 0; i < 2; i++) {
            lastMasterMessageRequestBody =
                OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                          orderWithUsers.getOrderId(),
                                                          orderWithUsers.getMasterToken());
            lastOwnerMessageRequestBody =
                OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                          orderWithUsers.getOrderId(),
                                                          orderWithUsers.getOrderOwnerToken());
        }

        List<OrderMessageDto> masterMessages =
            OrderMessagesControllerHelper.getOrderMessages(this.restTemplate,
                                                           orderWithUsers.getOrderId(),
                                                           orderWithUsers.getMasterToken());
        List<OrderMessageDto> orderOwnerMessages = OrderMessagesControllerHelper.getOrderMessages(
            this.restTemplate,
            orderWithUsers.getOrderId(),
            orderWithUsers.getOrderOwnerToken());

        assertEquals(6, masterMessages.size());
        assertEquals(6, orderOwnerMessages.size());

        assertEquals(lastOwnerMessageRequestBody.getText(), masterMessages.getLast().getText());
        assertEquals(lastMasterMessageRequestBody.getText(),
                     masterMessages.get(masterMessages.size() - 2).getText());
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"DRIVER", "CUSTOMER"})
    public void testGetOrderMessagesUsers(UserRole orderOwnerRole) {
        OrderWithUsersDto orderWithUsers = OrdersWorkflowTestHelper.createOrderWithAttachedUsers(
            this.usersService,
            this.userRepository,
            this.restTemplate,
            orderOwnerRole,
            null,
            null);

        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  orderWithUsers.getOrderId(),
                                                  orderWithUsers.getMasterToken());
        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  orderWithUsers.getOrderId(),
                                                  orderWithUsers.getOrderOwnerToken());

        for (String accessToken : Arrays.asList(orderWithUsers.getOrderOwnerToken(),
                                                orderWithUsers.getMasterToken())) {
            List<UserResponseDto> users =
                OrderMessagesControllerHelper.getOrderMessagesUsers(this.restTemplate,
                                                                    orderWithUsers.getOrderId(),
                                                                    accessToken);
            assertEquals(2, users.size());
        }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class,
                names = {"DRIVER", "CUSTOMER"})
    public void testSetMessagesViewedByRole(UserRole orderOwnerRole) {
        OrderWithUsersDto orderWithUsers = OrdersWorkflowTestHelper.createOrderWithAttachedUsers(
            this.usersService,
            this.userRepository,
            this.restTemplate,
            orderOwnerRole,
            null,
            null);

        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  orderWithUsers.getOrderId(),
                                                  orderWithUsers.getMasterToken());
        OrderMessagesControllerHelper.sendMessage(this.restTemplate,
                                                  orderWithUsers.getOrderId(),
                                                  orderWithUsers.getOrderOwnerToken());

        OrderMessagesControllerHelper.setMessagesViewedByRole(this.restTemplate,
                                                              orderWithUsers.getOrderId(),
                                                              orderWithUsers.getMasterToken());
        List<OrderMessageDto> messages =
            OrderMessagesControllerHelper.getOrderMessages(this.restTemplate,
                                                           orderWithUsers.getOrderId(),
                                                           orderWithUsers.getMasterToken());

        for (OrderMessageDto message : messages) {
            assertTrue(message.isViewedByMaster());
        }

        OrderMessagesControllerHelper.setMessagesViewedByRole(this.restTemplate,
                                                              orderWithUsers.getOrderId(),
                                                              orderWithUsers.getOrderOwnerToken());
        messages = OrderMessagesControllerHelper.getOrderMessages(this.restTemplate,
                                                                  orderWithUsers.getOrderId(),
                                                                  orderWithUsers.getMasterToken());

        for (OrderMessageDto message : messages) {
            assertTrue(message.isViewedByDriver() || message.isViewedByCustomer());
        }
    }
}
