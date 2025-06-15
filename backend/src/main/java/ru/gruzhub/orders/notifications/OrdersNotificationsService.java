package ru.gruzhub.orders.notifications;

import io.sentry.Sentry;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.gruzhub.orders.notifications.models.OldOrderNotification;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.repositories.OrderRepository;
import ru.gruzhub.orders.orders.repositories.OrderStatusChangeRepository;
import ru.gruzhub.telegram.models.TelegramChat;
import ru.gruzhub.telegram.services.TelegramSenderService;
import ru.gruzhub.users.UsersService;
import ru.gruzhub.users.models.User;

@Service
@RequiredArgsConstructor
public class OrdersNotificationsService {
    private final OrderRepository orderRepository;
    private final OldOrderNotificationRepository oldOrderNotificationRepository;
    private final OrderStatusChangeRepository orderStatusChangeRepository;
    private final TelegramSenderService telegramSenderService;
    private final UsersService usersService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        new Thread(() -> {
            while (true) {
                try {
                    this.sendNotifications();
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public void sendNotifications() {
        try {
            this.notifyAboutOldCreatedOrders();
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    private void notifyAboutOldCreatedOrders() {
        try {
            final long MINS_15_AGO_MS = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15);
            List<Order> pendingOrders =
                this.orderRepository.getCreatedOrdersUnchangedLongerThan(MINS_15_AGO_MS);

            List<User> admins = this.usersService.getAdmins();

            for (Order order : pendingOrders) {
                try {
                    OldOrderNotification notification =
                        this.oldOrderNotificationRepository.findByOrder(order);

                    if (notification == null) {
                        notification = new OldOrderNotification(null,
                                                                order,
                                                                order.getLastStatusUpdateTime(),
                                                                ZonedDateTime.now());
                        this.oldOrderNotificationRepository.save(notification);

                        for (User admin : admins) {
                            for (TelegramChat chat : admin.getConnectedTelegramChats()) {
                                this.telegramSenderService.sendMessage(chat.getTelegramChatId(),
                                                                       "Заказ #" +
                                                                       order.getId() +
                                                                       " находится в статусе " +
                                                                       "\"новая заявка\" " +
                                                                       "дольше 15 минут",
                                                                       null);
                            }
                        }
                    }
                } catch (Exception e) {
                    Sentry.captureException(e);
                }
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }
}
