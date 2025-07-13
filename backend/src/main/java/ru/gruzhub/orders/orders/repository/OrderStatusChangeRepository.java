package ru.gruzhub.orders.orders.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.model.OrderStatusChange;

@Repository
public interface OrderStatusChangeRepository extends JpaRepository<OrderStatusChange, Long> {
    List<OrderStatusChange> findStatusChangesByOrder(Order order);
}
