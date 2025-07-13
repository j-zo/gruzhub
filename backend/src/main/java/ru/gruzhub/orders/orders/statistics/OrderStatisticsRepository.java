package ru.gruzhub.orders.orders.statistics;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.gruzhub.orders.orders.model.Order;

@Repository
public interface OrderStatisticsRepository extends JpaRepository<Order, Long> {
    // Group by Day
    @Query(value = """
        WITH date_series AS (
            SELECT generate_series(
                CAST(MIN(TO_TIMESTAMP(o.created_at / 1000)) AS DATE), 
                CAST(MAX(TO_TIMESTAMP(o.created_at / 1000)) AS DATE), 
                INTERVAL '1 day'
            ) AS period
            FROM orders o
        )
        SELECT 
            ds.period AS period,
            COALESCE(COUNT(o.id), 0) AS count
        FROM date_series ds
        LEFT JOIN orders o 
        ON CAST(TO_TIMESTAMP(o.created_at / 1000) AS DATE) = ds.period
        GROUP BY ds.period
        ORDER BY ds.period
        """,
           nativeQuery = true)
    List<Object[]> findOrdersByDay();

    // Group by Week
    @Query(value = """
        WITH week_series AS (
            SELECT generate_series(
                CAST(DATE_TRUNC('week', MIN(TO_TIMESTAMP(o.created_at / 1000))) AS DATE), 
                CAST(DATE_TRUNC('week', MAX(TO_TIMESTAMP(o.created_at / 1000))) AS DATE), 
                INTERVAL '1 week'
            ) AS period
            FROM orders o
        )
        SELECT 
            ws.period AS period,
            COALESCE(COUNT(o.id), 0) AS count
        FROM week_series ws
        LEFT JOIN orders o 
        ON CAST(DATE_TRUNC('week', TO_TIMESTAMP(o.created_at / 1000)) AS DATE) = ws.period
        GROUP BY ws.period
        ORDER BY ws.period
        """,
           nativeQuery = true)
    List<Object[]> findOrdersByWeek();

    // Group by Month
    @Query(value = """
        WITH month_series AS (
            SELECT generate_series(
                CAST(DATE_TRUNC('month', MIN(TO_TIMESTAMP(o.created_at / 1000))) AS DATE), 
                CAST(DATE_TRUNC('month', MAX(TO_TIMESTAMP(o.created_at / 1000))) AS DATE), 
                INTERVAL '1 month'
            ) AS period
            FROM orders o
        )
        SELECT 
            ms.period AS period,
            COALESCE(COUNT(o.id), 0) AS count
        FROM month_series ms
        LEFT JOIN orders o 
        ON CAST(DATE_TRUNC('month', TO_TIMESTAMP(o.created_at / 1000)) AS DATE) = ms.period
        GROUP BY ms.period
        ORDER BY ms.period
        """,
           nativeQuery = true)
    List<Object[]> findOrdersByMonth();
}