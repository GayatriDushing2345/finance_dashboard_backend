package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.DashboardResponse;

/**
 * Contract for financial dashboard aggregation.
 *
 * Data visibility:
 * - EMPLOYEE → scoped to their own records only
 * - MANAGER / ADMIN → organisation-wide aggregates
 *
 * All monetary totals are derived from APPROVED expense records only.
 *
 * Implementation: {@link com.finance.dashboard.service.impl.DashboardServiceImpl}
 */
public interface DashboardService {

    /**
     * Assembles the full dashboard payload:
     * total income, total expenses, net balance, category breakdown,
     * monthly trend, recent transactions, and the monthly limit alert.
     */
    DashboardResponse getDashboard();
}
