package ru.gruzhub.orders.orders.service;

import io.sentry.Sentry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.repository.OrderRepository;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.enums.UserRole;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class OrdersBackgroundService {
    private static final long DAYS_2_AGO_MS = 2L * 24 * 60 * 60 * 1000;
    private static final long MONTH_AGO_MS = 31L * 24 * 60 * 60 * 1000;

    private static final String ADMIN_EMAIL = "rostislav.dugin@outlook.com";
    private static final String ADMIN_NAME = "Ростислав";
    private static final String ADMIN_PHONE = "79854776527";

    private final OrderRepository orderRepository;
    private final OrdersWorkflowService ordersWorkflowService;
    private final UsersService usersService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        this.usersService.createAdminUserIfNotExist(ADMIN_EMAIL, ADMIN_NAME, ADMIN_PHONE);

        new Thread(() -> {
            while (true) {
                try {
                    this.cancelOldNewOrders();
                    this.completeOldOrders();
                    Thread.sleep(5 * 60 * 1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void cancelOldNewOrders() {
        try {
            List<Order> outdatedOrders =
                this.orderRepository.findOrdersCreatedLongerThan(System.currentTimeMillis() -
                                                                 DAYS_2_AGO_MS);

            User adminUser = this.usersService.getUserByEmail(ADMIN_EMAIL, UserRole.ADMIN);
            if (adminUser == null) {
                throw new IllegalStateException("Admin user is not initialized");
            }

            for (Order outdatedOrder : outdatedOrders) {
                try {
                    this.ordersWorkflowService.cancelOrder(adminUser,
                                                           outdatedOrder.getId(),
                                                           "Order not processed within 48 hours");
                } catch (Exception e) {
                    Sentry.captureException(e);
                }
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    public void completeOldOrders() {
        try {
            List<Order> outdatedOrders =
                this.orderRepository.findOrdersInProgressChangedLongerThan(System.currentTimeMillis() -
                                                                           MONTH_AGO_MS);

            User adminUser = this.usersService.getUserByEmail(ADMIN_EMAIL, UserRole.ADMIN);

            if (adminUser == null) {
                throw new IllegalStateException("Admin user is not initialized");
            }

            for (Order outdatedOrder : outdatedOrders) {
                try {
                    this.ordersWorkflowService.completeOrder(adminUser, outdatedOrder.getId());
                } catch (Exception e) {
                    Sentry.captureException(e);
                }
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }
}

