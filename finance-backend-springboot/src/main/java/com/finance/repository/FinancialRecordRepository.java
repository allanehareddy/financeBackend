package com.finance.repository;

import com.finance.entity.FinancialRecord;
import com.finance.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // ── Filtered listing with optional keyword search ─────────────────────────

    @Query("""
        SELECT r FROM FinancialRecord r
        WHERE r.deleted = false
          AND (:type      IS NULL OR r.type      = :type)
          AND (:category  IS NULL OR r.category  = :category)
          AND (:startDate IS NULL OR r.date     >= :startDate)
          AND (:endDate   IS NULL OR r.date     <= :endDate)
          AND (:search    IS NULL
               OR LOWER(r.category) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.notes)    LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<FinancialRecord> findAllFiltered(
            @Param("type")      TransactionType type,
            @Param("category")  String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("search")    String search,
            Pageable pageable
    );

    // ── Dashboard aggregations ─────────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r WHERE r.deleted = false AND r.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("""
        SELECT r.category, r.type, SUM(r.amount), COUNT(r)
        FROM FinancialRecord r
        WHERE r.deleted = false
        GROUP BY r.category, r.type
        ORDER BY SUM(r.amount) DESC
        """)
    List<Object[]> categoryTotals();

    @Query("""
        SELECT r FROM FinancialRecord r
        WHERE r.deleted = false
        ORDER BY r.createdAt DESC
        """)
    List<FinancialRecord> findRecentActivity(Pageable pageable);

    @Query("""
        SELECT FUNCTION('FORMATDATETIME', r.date, 'yyyy-MM') AS month,
               SUM(CASE WHEN r.type = 'INCOME'  THEN r.amount ELSE 0 END),
               SUM(CASE WHEN r.type = 'EXPENSE' THEN r.amount ELSE 0 END)
        FROM FinancialRecord r
        WHERE r.deleted = false AND r.date >= :since
        GROUP BY FUNCTION('FORMATDATETIME', r.date, 'yyyy-MM')
        ORDER BY month ASC
        """)
    List<Object[]> monthlyTrends(@Param("since") LocalDate since);
}
