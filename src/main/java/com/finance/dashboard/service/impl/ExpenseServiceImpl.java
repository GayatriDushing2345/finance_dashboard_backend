package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.request.ApprovalRequest;
import com.finance.dashboard.dto.request.ExpenseRequest;
import com.finance.dashboard.dto.response.ExpenseResponse;
import com.finance.dashboard.entity.Category;
import com.finance.dashboard.entity.Expense;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.ExpenseStatus;
import com.finance.dashboard.exception.AppException;
import com.finance.dashboard.repository.ExpenseRepository;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.service.ExpenseService;
import com.finance.dashboard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Core expense business logic.
 *
 * Access control (3-layer defence-in-depth):
 *   Layer 1 – URL pattern in SecurityConfig
 *   Layer 2 – @PreAuthorize on controller methods
 *   Layer 3 – Explicit role checks here in the service
 *
 * Workflow:
 *   Create  → status = PENDING
 *   Review  → PENDING → APPROVED | REJECTED (Manager/Admin only)
 *   Guard   → APPROVED expenses are immutable (no edit, no delete)
 *
 * CategoryServiceImpl is injected directly (not via the CategoryService interface)
 * because findEntityById() is a package-level helper method not declared on the interface.
 * Both classes live in the same package, so this is an intentional, contained coupling.
 */
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository  expenseRepository;
    private final UserRepository     userRepository;
    private final CategoryServiceImpl categoryService;
    private final SecurityUtil       securityUtil;

    @Value("${app.monthly-limit:10000.00}")
    private BigDecimal monthlyLimit;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest.Create request) {
        User     currentUser = findUser(securityUtil.getCurrentUserId());
        Category category    = categoryService.findEntityById(request.getCategoryId());

        Expense expense = Expense.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(category)
                .user(currentUser)
                .date(request.getDate())
                .note(request.getNote())
                .status(ExpenseStatus.PENDING)
                .createdByUser(currentUser)
                .build();

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = findExpense(id);
        checkReadAccess(expense);
        return ExpenseResponse.from(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(ExpenseRequest.Filter filter, Pageable pageable) {
        Long scopedUserId = securityUtil.isEmployee()
                ? securityUtil.getCurrentUserId()
                : null;

        ExpenseStatus statusFilter = parseStatus(filter.getStatus());

        if (filter.getStartDate() != null && filter.getEndDate() != null
                && filter.getStartDate().isAfter(filter.getEndDate())) {
            throw new AppException.BadRequestException(
                    "startDate must be before or equal to endDate");
        }

        if (filter.getCategoryId() != null) {
            categoryService.findEntityById(filter.getCategoryId());
        }

        return expenseRepository
                .findWithFilters(
                        scopedUserId,
                        filter.getCategoryId(),
                        filter.getType(),
                        statusFilter,
                        filter.getStartDate(),
                        filter.getEndDate(),
                        pageable)
                .map(ExpenseResponse::from);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest.Update request) {
        Expense expense = findExpense(id);
        checkWriteAccess();
        guardApproved(expense, "edited");

        if (request.getAmount()     != null) expense.setAmount(request.getAmount());
        if (request.getType()       != null) expense.setType(request.getType());
        if (request.getDate()       != null) expense.setDate(request.getDate());
        if (request.getNote()       != null) expense.setNote(request.getNote());
        if (request.getCategoryId() != null) {
            expense.setCategory(categoryService.findEntityById(request.getCategoryId()));
        }

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = findExpense(id);
        checkWriteAccess();
        guardApproved(expense, "deleted");
        expenseRepository.delete(expense);
    }

    // ── REVIEW ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ExpenseResponse reviewExpense(Long id, ApprovalRequest request) {
        if (!securityUtil.isManagerOrAdmin()) {
            throw new AppException.AccessDeniedException(
                    "Only Managers and Admins can review expenses.");
        }

        if (request.getStatus() == ExpenseStatus.PENDING) {
            throw new AppException.BadRequestException(
                    "Review status must be APPROVED or REJECTED, not PENDING.");
        }

        Expense expense = findExpense(id);

        if (!expense.isPending()) {
            throw new AppException.BadRequestException(
                    "Only PENDING expenses can be reviewed. Current status: " + expense.getStatus());
        }

        expense.setStatus(request.getStatus());
        expense.setReviewedByUser(findUser(securityUtil.getCurrentUserId()));
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    // ── MONTHLY LIMIT ALERT ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public String checkMonthlyLimitAlert(Long userId) {
        LocalDate  now      = LocalDate.now();
        BigDecimal spending = expenseRepository.sumMonthlyExpensesByUser(
                now.getYear(), now.getMonthValue(), userId);

        if (spending != null && spending.compareTo(monthlyLimit) > 0) {
            return String.format(
                    "Monthly spending limit exceeded! You have spent %.2f of your %.2f limit this month.",
                    spending, monthlyLimit);
        }
        return null;
    }

    // ── Access control ────────────────────────────────────────────────────────

    private void checkReadAccess(Expense expense) {
        if (securityUtil.isEmployee()
                && !expense.getUser().getId().equals(securityUtil.getCurrentUserId())) {
            throw new AppException.AccessDeniedException(
                    "You can only view your own expenses.");
        }
    }

    private void checkWriteAccess() {
        if (securityUtil.isEmployee()) {
            throw new AppException.AccessDeniedException(
                    "Employees are not permitted to edit or delete expenses.");
        }
    }

    private void guardApproved(Expense expense, String action) {
        if (expense.isApproved()) {
            throw new AppException.BadRequestException(
                    "Approved expenses cannot be " + action + ". Current status: APPROVED");
        }
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private Expense findExpense(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("Expense", id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("User", id));
    }

    private ExpenseStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ExpenseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException(
                    "Invalid status value: '" + status +
                    "'. Allowed values: PENDING, APPROVED, REJECTED");
        }
    }
}
