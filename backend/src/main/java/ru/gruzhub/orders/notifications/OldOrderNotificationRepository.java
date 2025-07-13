package ru.gruzhub.orders.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gruzhub.orders.notifications.models.OldOrderNotification;
import ru.gruzhub.orders.orders.model.Order;

public interface OldOrderNotificationRepository extends JpaRepository<OldOrderNotification, Long> {
    OldOrderNotification findByOrder(Order order);
}