package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.ApprovalRequest;
import com.finance.dashboard.dto.request.ExpenseRequest;
import com.finance.dashboard.dto.response.ExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for expense management and the approval workflow.
 *
 * Access rules (enforced in implementation):
 * - Create / read: all authenticated roles
 * - EMPLOYEE read: scoped to own expenses only
 * - Update / delete: MANAGER or ADMIN only; blocked for APPROVED expenses
 * - Review (approve/reject): MANAGER or ADMIN only; only PENDING expenses may be reviewed
 *
 * Implementation: {@link com.finance.dashboard.service.impl.ExpenseServiceImpl}
 *
 * Refactoring note:
 * {@code checkMonthlyLimitAlert()} was originally a public method on the concrete
 * {@code ExpenseServiceImpl} class, forcing {@code ExpenseController} to inject
 * the implementation rather than this interface (breaking the abstraction).
 * Moving it here allows the controller to depend only on the interface.
 */
public interface ExpenseService {

    /** Creates a new expense with status=PENDING for the currently authenticated user. */
    ExpenseResponse createExpense(ExpenseRequest.Create request);

    /**
     * Returns an expense by ID.
     * EMPLOYEE callers may only retrieve their own expense (access check inside).
     */
    ExpenseResponse getExpenseById(Long id);

    /**
     * Returns a paginated, filtered list of expenses.
     * EMPLOYEE callers are automatically scoped to their own records.
     * MANAGER / ADMIN callers see all records.
     */
    Page<ExpenseResponse> getExpenses(ExpenseRequest.Filter filter, Pageable pageable);

    /**
     * Applies a partial update to an expense (non-null fields only).
     * Restricted to MANAGER / ADMIN. Blocked if the expense is APPROVED.
     */
    ExpenseResponse updateExpense(Long id, ExpenseRequest.Update request);

    /**
     * Deletes an expense.
     * Restricted to MANAGER / ADMIN. Blocked if the expense is APPROVED.
     */
    void deleteExpense(Long id);

    /**
     * Sets an expense status to APPROVED or REJECTED.
     * Restricted to MANAGER / ADMIN. Only PENDING expenses may be reviewed.
     */
    ExpenseResponse reviewExpense(Long id, ApprovalRequest request);

    /**
     * Checks whether the given user's EXPENSE-type spending for the current month
     * exceeds the configured monthly limit ({@code app.monthly-limit}).
     *
     * @return A human-readable alert string if the limit is exceeded; {@code null} otherwise.
     */
    String checkMonthlyLimitAlert(Long userId);
}
