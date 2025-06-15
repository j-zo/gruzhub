package ru.gruzhub.orders.orders.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.gruzhub.orders.orders.models.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findOrderByGuaranteeUuid(String guaranteeUuid);

    @Query(value = """
        SELECT * FROM orders
        WHERE (master_id = :masterId AND (status IN :statuses OR :statuses IS NULL))
           OR (address_id IN (SELECT id FROM address WHERE region_id = :regionId) AND status = 'CREATED')
        ORDER BY
            CASE
                WHEN status = 'CREATED' THEN 1
                WHEN status = 'CALCULATING' THEN 2
                WHEN status = 'REVIEWING' THEN 3
                WHEN status = 'ACCEPTED' THEN 4
                WHEN status = 'COMPLETED' THEN 5
                WHEN status = 'CANCEL' THEN 6
            END,
            id DESC
        LIMIT :limit
        """,
           nativeQuery = true)
    List<Order> findMasterOrders(Long masterId,
                                 Long regionId,
                                 List<String> statuses,
                                 Integer limit);

    @Query(value = """
        SELECT * FROM orders
        WHERE (driver_id = :driverId AND (status IN :statuses OR :statuses IS NULL))
           OR (address_id IN (SELECT id FROM address WHERE region_id = :regionId) AND status = 'CREATED')
        ORDER BY
            CASE
                WHEN status = 'CREATED' THEN 1
                WHEN status = 'CALCULATING' THEN 2
                WHEN status = 'REVIEWING' THEN 3
                WHEN status = 'ACCEPTED' THEN 4
                WHEN status = 'COMPLETED' THEN 5
                WHEN status = 'CANCEL' THEN 6
            END,
            id DESC
        LIMIT :limit
        """,
           nativeQuery = true)
    List<Order> driverOrders(Long driverId, Long regionId, List<String> statuses, Integer limit);

    @Query(value = """
        SELECT * FROM orders
        WHERE status = 'CREATED' AND last_status_update_time < :time
        ORDER BY id DESC
        """,
           nativeQuery = true)
    List<Order> findOrdersCreatedLongerThan(Long time);

    @Query(value = """
        SELECT * FROM orders
        WHERE status IN ('CALCULATING', 'REVIEWING', 'ACCEPTED') AND last_status_update_time < :time
        ORDER BY id DESC
        """,
           nativeQuery = true)
    List<Order> findOrdersInProgressChangedLongerThan(Long time);

    @Query(value = """
        SELECT orders.* FROM orders
        JOIN order_to_auto_assosiation ota ON orders.id = ota.order_id
        WHERE ota.auto_id = :autoId
        """,
           nativeQuery = true)
    List<Order> findOrdersByAuto(Long autoId);

    @Query(value = """
        SELECT * FROM orders
        WHERE status = 'CREATED' AND last_status_update_time < :time
        ORDER BY id DESC
        """,
           nativeQuery = true)
    List<Order> getCreatedOrdersUnchangedLongerThan(Long time);

    @Query(value = """
        SELECT * FROM orders
        WHERE status IN ('CALCULATING', 'REVIEWING', 'ACCEPTED') AND last_status_update_time < :time
        ORDER BY id DESC
        """,
           nativeQuery = true)
    List<Order> getActiveOrdersChangedLongerThan(Long time);
}
