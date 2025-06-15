package ru.gruzhub.users.statistics;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.gruzhub.users.models.User;

@Repository
public interface UserStatisticsRepository extends JpaRepository<User, Long> {
    // Group by Day
    @Query(value = """
        WITH date_series AS (
            SELECT generate_series(
                CAST(MIN(TO_TIMESTAMP(u.registration_date / 1000)) AS DATE), 
                CAST(MAX(TO_TIMESTAMP(u.registration_date / 1000)) AS DATE), 
                INTERVAL '1 day'
            ) AS period
            FROM users u
        )
        SELECT 
            ds.period AS period,
            COALESCE(COUNT(u.id), 0) AS count
        FROM date_series ds
        LEFT JOIN users u 
        ON CAST(TO_TIMESTAMP(u.registration_date / 1000) AS DATE) = ds.period
        GROUP BY ds.period
        ORDER BY ds.period
        """,
           nativeQuery = true)
    List<Object[]> findRegistrationsByDay();

    // Group by Week
    @Query(value = """
        WITH week_series AS (
            SELECT generate_series(
                CAST(DATE_TRUNC('week', MIN(TO_TIMESTAMP(u.registration_date / 1000))) AS DATE), 
                CAST(DATE_TRUNC('week', MAX(TO_TIMESTAMP(u.registration_date / 1000))) AS DATE), 
                INTERVAL '1 week'
            ) AS period
            FROM users u
        )
        SELECT 
            ws.period AS period,
            COALESCE(COUNT(u.id), 0) AS count
        FROM week_series ws
        LEFT JOIN users u 
        ON CAST(DATE_TRUNC('week', TO_TIMESTAMP(u.registration_date / 1000)) AS DATE) = ws.period
        GROUP BY ws.period
        ORDER BY ws.period
        """,
           nativeQuery = true)
    List<Object[]> findRegistrationsByWeek();

    // Group by Month
    @Query(value = """
        WITH month_series AS (
            SELECT generate_series(
                CAST(DATE_TRUNC('month', MIN(TO_TIMESTAMP(u.registration_date / 1000))) AS DATE), 
                CAST(DATE_TRUNC('month', MAX(TO_TIMESTAMP(u.registration_date / 1000))) AS DATE), 
                INTERVAL '1 month'
            ) AS period
            FROM users u
        )
        SELECT 
            ms.period AS period,
            COALESCE(COUNT(u.id), 0) AS count
        FROM month_series ms
        LEFT JOIN users u 
        ON CAST(DATE_TRUNC('month', TO_TIMESTAMP(u.registration_date / 1000)) AS DATE) = ms.period
        GROUP BY ms.period
        ORDER BY ms.period
        """,
           nativeQuery = true)
    List<Object[]> findRegistrationsByMonth();
}
