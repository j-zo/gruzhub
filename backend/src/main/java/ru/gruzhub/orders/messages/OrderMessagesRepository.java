package ru.gruzhub.orders.messages;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.gruzhub.orders.messages.models.OrderMessage;

@Repository
public interface OrderMessagesRepository extends JpaRepository<OrderMessage, Long> {

    Optional<OrderMessage> findByOrderIdAndGuaranteeId(Long orderId, String guaranteeId);

    @Query(value = """
        SELECT DISTINCT ON (om.order_id) om.* FROM order_messages om
        JOIN orders o ON om.order_id = o.id
        WHERE om.order_id IN (:ordersIds) AND (o.master_id = :userId OR o.driver_id = :userId OR o.customer_id = :userId)
        ORDER BY om.order_id, om.date DESC
        """,
           nativeQuery = true)
    List<OrderMessage> findLastMessagesPerOrder(Long userId, List<Long> ordersIds);

    @Query("SELECT om FROM OrderMessage om " +
           "WHERE om.order.id = :orderId " +
           "ORDER BY om.date ASC")
    List<OrderMessage> findOrderMessages(Long orderId);

    @Query("SELECT DISTINCT om.user.id FROM OrderMessage om " + "WHERE om.order.id = :orderId")
    List<Long> findOrderMessagesUserIds(Long orderId);
}