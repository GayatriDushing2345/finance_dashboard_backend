package com.finance.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated financial dashboard payload.
 *
 * All monetary totals are derived from APPROVED expense records only.
 * Returned by {@code GET /api/dashboard} – scoped to the current user
 * if EMPLOYEE, or organisation-wide if MANAGER / ADMIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // ── Top-line aggregates ───────────────────────────────────────────────────
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    // ── Breakdowns ────────────────────────────────────────────────────────────
    private List<CategorySummary> categoryTotals;
    private List<MonthlySummary>  monthlySummaries;
    private List<ExpenseResponse> recentTransactions;

    // ── Budget alert ──────────────────────────────────────────────────────────
    private LimitAlert limitAlert;

    // ── Nested types ──────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
         private String     categoryName;
        private String     type;          // "INCOME" or "EXPENSE"
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySummary {
         private int        year;
        private int        month;
         private String     monthName;     // e.g. "APRIL"
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal netBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitAlert {
        private boolean    exceeded;
        private BigDecimal monthlyLimit;
        private BigDecimal currentSpending;
         private String     message;
    }
}
