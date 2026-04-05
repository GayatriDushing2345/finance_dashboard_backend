package com.finance.dashboard.repository;

import com.finance.dashboard.entity.Expense;
import com.finance.dashboard.enums.ExpenseStatus;
import com.finance.dashboard.enums.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access layer for {@link Expense} entities.
 *
 * Query design decisions:
 * - All monetary aggregations use {@code COALESCE(..., 0)} to return 0 instead of null
 *   when no rows match, preventing NullPointerExceptions in the service layer.
 * - Dashboard queries operate only on APPROVED records to avoid distorting summaries
 *   with unreviewed or rejected data.
 * - The main filter query uses nullable parameters so all filters are optional;
 *   a null parameter means "no filter applied for this field".
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ── Basic filters ─────────────────────────────────────────────────────────

    Page<Expense> findByUserId(Long userId, Pageable pageable);

    Page<Expense> findByUserIdAndStatus(Long userId, ExpenseStatus status, Pageable pageable);

    Page<Expense> findByStatus(ExpenseStatus status, Pageable pageable);

    // ── Aggregate totals (APPROVED only) ─────────────────────────────────────

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.type = :type AND e.status = 'APPROVED'")
    BigDecimal sumByType(@Param("type") ExpenseType type);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.type = :type AND e.status = 'APPROVED' AND e.user.id = :userId")
    BigDecimal sumByTypeAndUserId(@Param("type") ExpenseType type, @Param("userId") Long userId);

    // ── Monthly spending (for limit alert) ───────────────────────────────────

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.type = 'EXPENSE' AND e.status = 'APPROVED' " +
           "AND YEAR(e.date) = :year AND MONTH(e.date) = :month AND e.user.id = :userId")
    BigDecimal sumMonthlyExpensesByUser(
            @Param("year") int year,
            @Param("month") int month,
            @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.type = 'EXPENSE' AND e.status = 'APPROVED' " +
           "AND YEAR(e.date) = :year AND MONTH(e.date) = :month")
    BigDecimal sumMonthlyExpenses(@Param("year") int year, @Param("month") int month);

    // ── Category-wise totals (dashboard breakdown) ────────────────────────────

    @Query("SELECT e.category.name, e.type, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.status = 'APPROVED' GROUP BY e.category.name, e.type")
    List<Object[]> getCategoryWiseTotals();

    @Query("SELECT e.category.name, e.type, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.status = 'APPROVED' AND e.user.id = :userId " +
           "GROUP BY e.category.name, e.type")
    List<Object[]> getCategoryWiseTotalsByUser(@Param("userId") Long userId);

    // ── Monthly trend summary (for chart rendering) ───────────────────────────

    @Query("SELECT YEAR(e.date), MONTH(e.date), e.type, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.status = 'APPROVED' " +
           "GROUP BY YEAR(e.date), MONTH(e.date), e.type " +
           "ORDER BY YEAR(e.date) DESC, MONTH(e.date) DESC")
    List<Object[]> getMonthlySummary();

    @Query("SELECT YEAR(e.date), MONTH(e.date), e.type, COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.status = 'APPROVED' AND e.user.id = :userId " +
           "GROUP BY YEAR(e.date), MONTH(e.date), e.type " +
           "ORDER BY YEAR(e.date) DESC, MONTH(e.date) DESC")
    List<Object[]> getMonthlySummaryByUser(@Param("userId") Long userId);

    // ── Recent transactions (dashboard feed) ─────────────────────────────────

    @Query("SELECT e FROM Expense e ORDER BY e.createdAt DESC")
    List<Expense> findRecentTransactions(Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    List<Expense> findRecentTransactionsByUser(@Param("userId") Long userId, Pageable pageable);

    // ── Universal filter query ────────────────────────────────────────────────

    /**
     * Fetches expenses with all optional filters applied.
     * Null parameters are ignored (treated as "match everything").
     *
     * @param userId     null = all users (MANAGER/ADMIN); non-null = scoped to one user (EMPLOYEE)
     * @param categoryId null = all categories
     * @param type       null = INCOME + EXPENSE combined
     * @param status     null = all statuses
     * @param startDate  null = no lower date bound
     * @param endDate    null = no upper date bound
     */
    @Query("SELECT e FROM Expense e WHERE " +
           "(:userId IS NULL OR e.user.id = :userId) AND " +
           "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:startDate IS NULL OR e.date >= :startDate) AND " +
           "(:endDate IS NULL OR e.date <= :endDate)")
    Page<Expense> findWithFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("type") ExpenseType type,
            @Param("status") ExpenseStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
}
