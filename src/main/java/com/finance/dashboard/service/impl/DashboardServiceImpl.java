package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.response.DashboardResponse;
import com.finance.dashboard.dto.response.ExpenseResponse;
import com.finance.dashboard.enums.ExpenseType;
import com.finance.dashboard.repository.ExpenseRepository;
import com.finance.dashboard.service.DashboardService;
import com.finance.dashboard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembles the financial dashboard for the currently authenticated user.
 *
 * Scoping rules:
 * - EMPLOYEE  → sees only their own approved transactions.
 * - MANAGER / ADMIN → sees the entire organisation's approved transactions.
 *
 * All monetary aggregations use APPROVED records only,
 * so PENDING and REJECTED expenses never distort the summary.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ExpenseRepository expenseRepository;
    private final SecurityUtil      securityUtil;

    @Value("${app.monthly-limit:10000.00}")
    private BigDecimal monthlyLimit;

    private static final int RECENT_TX_LIMIT = 10;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        boolean isEmployee = securityUtil.isEmployee();
        Long    userId     = securityUtil.getCurrentUserId();

        // ── Top-line totals ────────────────────────────────────────────────────
        BigDecimal totalIncome = isEmployee
                ? expenseRepository.sumByTypeAndUserId(ExpenseType.INCOME, userId)
                : expenseRepository.sumByType(ExpenseType.INCOME);

        BigDecimal totalExpenses = isEmployee
                ? expenseRepository.sumByTypeAndUserId(ExpenseType.EXPENSE, userId)
                : expenseRepository.sumByType(ExpenseType.EXPENSE);

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        // ── Category breakdown ─────────────────────────────────────────────────
        List<Object[]> catRows = isEmployee
                ? expenseRepository.getCategoryWiseTotalsByUser(userId)
                : expenseRepository.getCategoryWiseTotals();

        List<DashboardResponse.CategorySummary> categoryTotals = catRows.stream()
                .map(row -> DashboardResponse.CategorySummary.builder()
                        .categoryName((String) row[0])
                        .type(row[1].toString())
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        // ── Monthly trend (for chart rendering) ───────────────────────────────
        List<Object[]> monthRows = isEmployee
                ? expenseRepository.getMonthlySummaryByUser(userId)
                : expenseRepository.getMonthlySummary();

        // Merge INCOME and EXPENSE rows for the same (year, month) key into one object
        Map<String, DashboardResponse.MonthlySummary> monthMap = new LinkedHashMap<>();
        for (Object[] row : monthRows) {
            int        year   = ((Number) row[0]).intValue();
            int        month  = ((Number) row[1]).intValue();
            String     type   = row[2].toString();
            BigDecimal amount = (BigDecimal) row[3];

            String key = year + "-" + month;
            DashboardResponse.MonthlySummary ms = monthMap.computeIfAbsent(key, k ->
                    DashboardResponse.MonthlySummary.builder()
                            .year(year)
                            .month(month)
                            .monthName(Month.of(month).name())
                            .totalIncome(BigDecimal.ZERO)
                            .totalExpenses(BigDecimal.ZERO)
                            .netBalance(BigDecimal.ZERO)
                            .build());

            if ("INCOME".equals(type)) {
                ms.setTotalIncome(amount);
            } else {
                ms.setTotalExpenses(amount);
            }
            ms.setNetBalance(ms.getTotalIncome().subtract(ms.getTotalExpenses()));
        }

        // ── Recent transactions ────────────────────────────────────────────────
        List<ExpenseResponse> recent = isEmployee
                ? expenseRepository
                        .findRecentTransactionsByUser(userId, PageRequest.of(0, RECENT_TX_LIMIT))
                        .stream().map(ExpenseResponse::from).collect(Collectors.toList())
                : expenseRepository
                        .findRecentTransactions(PageRequest.of(0, RECENT_TX_LIMIT))
                        .stream().map(ExpenseResponse::from).collect(Collectors.toList());

        // ── Monthly limit alert ────────────────────────────────────────────────
        // The limit alert is always scoped to the individual user (not org-wide),
        // even for Managers/Admins, so they can monitor their own spending.
        LocalDate  now              = LocalDate.now();
        BigDecimal currentMonthSpend = expenseRepository.sumMonthlyExpensesByUser(
                now.getYear(), now.getMonthValue(), userId);

        boolean exceeded = currentMonthSpend.compareTo(monthlyLimit) > 0;
        DashboardResponse.LimitAlert alert = DashboardResponse.LimitAlert.builder()
                .exceeded(exceeded)
                .monthlyLimit(monthlyLimit)
                .currentSpending(currentMonthSpend)
                .message(exceeded
                        ? String.format("Monthly limit exceeded! Spent %.2f of %.2f limit.",
                                currentMonthSpend, monthlyLimit)
                        : String.format("Within budget. Spent %.2f of %.2f limit.",
                                currentMonthSpend, monthlyLimit))
                .build();

        return DashboardResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .monthlySummaries(new ArrayList<>(monthMap.values()))
                .recentTransactions(recent)
                .limitAlert(alert)
                .build();
    }
}
