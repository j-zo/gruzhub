package ru.gruzhub.orders.orders.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.orders.orders.enums.OrderStatus;
import ru.gruzhub.orders.orders.model.Order;

@Repository
public class OrderQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves a list of orders for a specific master within a given region,
     * filtered by statuses and limited by the specified number.
     *
     * @param masterId the ID of the master
     * @param regionId the ID of the region
     * @param statuses the list of order statuses to filter by
     * @param limit    the maximum number of orders to retrieve
     * @return a list of matching Order entities
     */
    public List<Order> findMasterOrders(Long masterId,
                                        Long regionId,
                                        List<OrderStatus> statuses,
                                        Integer limit) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();

        if (statuses == null || statuses.isEmpty()) {
            // If no statuses provided, include orders where the master is assigned
            // or the order is in the specified region with status CREATED
            predicates.add(cb.or(cb.equal(orderRoot.get("master").get("id"), masterId),
                                 cb.and(cb.equal(orderRoot.get("address").get("region").get("id"),
                                                 regionId),
                                        cb.equal(orderRoot.get("status"), OrderStatus.CREATED))));
        } else if (statuses.contains(OrderStatus.CREATED)) {
            // If CREATED status is among the statuses, include orders where:
            // - Master is assigned and status is in the provided statuses
            // - Order is in the specified region with status CREATED
            predicates.add(cb.or(cb.and(cb.equal(orderRoot.get("master").get("id"), masterId),
                                        orderRoot.get("status").in(statuses)),
                                 cb.and(cb.equal(orderRoot.get("address").get("region").get("id"),
                                                 regionId),
                                        cb.equal(orderRoot.get("status"), OrderStatus.CREATED))));
        } else {
            // If CREATED status is not among the statuses, include orders
            // where the master is assigned and status is in the provided statuses
            predicates.add(cb.and(cb.equal(orderRoot.get("master").get("id"), masterId),
                                  orderRoot.get("status").in(statuses)));
        }

        // Build the query with the specified predicates and ordering
        cq.select(orderRoot)
          .where(predicates.toArray(new Predicate[0]))
          .orderBy(cb.asc(cb.selectCase(orderRoot.get("status"))
                            .when(OrderStatus.CREATED, 1)
                            .when(OrderStatus.CALCULATING, 2)
                            .when(OrderStatus.REVIEWING, 3)
                            .when(OrderStatus.ACCEPTED, 4)
                            .when(OrderStatus.COMPLETED, 5)
                            .when(OrderStatus.CANCEL, 6)
                            .otherwise(7)), cb.desc(orderRoot.get("id")));

        TypedQuery<Order> query = this.entityManager.createQuery(cq);
        if (limit != null && limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }

    /**
     * Retrieves a list of orders based on various optional filters such as master ID,
     * customer ID, driver ID, transport ID, user ID, region IDs, statuses, and a limit.
     *
     * @param masterId   the ID of the master (optional)
     * @param customerId the ID of the customer (optional)
     * @param driverId   the ID of the driver (optional)
     * @param transportId     the ID of the transport (optional)
     * @param userId     the ID of the user (optional)
     * @param regionIds  the list of region IDs to filter by (optional)
     * @param statuses   the list of order statuses to filter by (optional)
     * @param limit      the maximum number of orders to retrieve (optional)
     * @return a list of matching Order entities
     */
    public List<Order> findOrders(Long masterId,
                                  Long customerId,
                                  Long driverId,
                                  Long transportId,
                                  Long userId,
                                  List<Long> regionIds,
                                  List<OrderStatus> statuses,
                                  Integer limit) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();

        // Filter by statuses if provided
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(orderRoot.get("status").in(statuses));
        }

        // Filter by transport ID if provided (join with transport)
        if (transportId != null) {
            Join<Order, Transport> transportJoin = orderRoot.join("transport", JoinType.INNER);
            predicates.add(cb.equal(transportJoin.get("id"), transportId));
        }

        // Filter by customer ID if provided
        if (customerId != null) {
            predicates.add(cb.equal(orderRoot.get("customer").get("id"), customerId));
        }

        // Filter by driver ID if provided
        if (driverId != null) {
            predicates.add(cb.equal(orderRoot.get("driver").get("id"), driverId));
        }

        // Filter by user ID if provided (customer, driver, or master)
        if (userId != null) {
            predicates.add(cb.or(cb.equal(orderRoot.get("customer").get("id"), userId),
                                 cb.equal(orderRoot.get("driver").get("id"), userId),
                                 cb.equal(orderRoot.get("master").get("id"), userId)));
        }

        // Filter by region IDs if provided
        if (regionIds != null && !regionIds.isEmpty()) {
            predicates.add(orderRoot.get("address").get("region").get("id").in(regionIds));
        }

        // Filter by master ID if provided
        if (masterId != null) {
            predicates.add(cb.equal(orderRoot.get("master").get("id"), masterId));
        }

        // Build the query with the specified predicates and ordering
        cq.select(orderRoot)
          .where(predicates.toArray(new Predicate[0]))
          .orderBy(cb.asc(cb.selectCase(orderRoot.get("status"))
                            .when(OrderStatus.CREATED, 1)
                            .when(OrderStatus.CALCULATING, 2)
                            .when(OrderStatus.REVIEWING, 3)
                            .when(OrderStatus.ACCEPTED, 4)
                            .when(OrderStatus.COMPLETED, 5)
                            .when(OrderStatus.CANCEL, 6)
                            .otherwise(7)), cb.desc(orderRoot.get("id")));

        TypedQuery<Order> query = this.entityManager.createQuery(cq);
        if (limit != null && limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }
}