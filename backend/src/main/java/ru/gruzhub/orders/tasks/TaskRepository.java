package ru.gruzhub.orders.tasks;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gruzhub.orders.tasks.models.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    /**
     * Finds tasks by order ID and transport ID, sorted as specified.
     *
     * @param orderId the ID of the order
     * @param transportId  the ID of the transport
     * @param sort    the sorting criteria
     * @return a list of matching Task entities
     */
    List<Task> findByOrderIdAndTransportId(Long orderId, Long transportId, Sort sort);

    /**
     * Finds tasks by order ID, sorted as specified.
     *
     * @param orderId the ID of the order
     * @param sort    the sorting criteria
     * @return a list of matching Task entities
     */
    List<Task> findByOrderId(Long orderId, Sort sort);
}
