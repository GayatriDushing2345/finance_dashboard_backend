package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.ApprovalRequest;
import com.finance.dashboard.dto.request.ExpenseRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.ExpenseResponse;
import com.finance.dashboard.enums.ExpenseType;
import com.finance.dashboard.service.ExpenseService;
import com.finance.dashboard.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Expense management REST controller.

 * Role-based access summary:
 *   EMPLOYEE  → create expenses; read own expenses only
 *   MANAGER   → read all; edit/delete non-APPROVED; approve/reject PENDING
 *   ADMIN     → same as MANAGER + full user management (handled in UserController).
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Financial record management with approval workflow")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final SecurityUtil   securityUtil;

    // ── List / filter ─────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List expenses with optional filters (Employees see own; Managers/Admins see all)")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpenses(
            @RequestParam(required = false) Long        categoryId,
            @RequestParam(required = false) ExpenseType type,
            @RequestParam(required = false) String      status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {

        ExpenseRequest.Filter filter = new ExpenseRequest.Filter();
        filter.setCategoryId(categoryId);
        filter.setType(type);
        filter.setStatus(status);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));

        return ResponseEntity.ok(
                ApiResponse.success(expenseService.getExpenses(filter, pageable),
                        "Expenses retrieved successfully"));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID (Employees can only access their own)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpenseById(id)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a new expense record (status = PENDING).
     * After creation, checks whether the user's monthly EXPENSE total
     * exceeds the configured limit and attaches an alert string to the response if so.
     */
    @PostMapping
    @Operation(summary = "Create a new expense – always submitted as PENDING")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody ExpenseRequest.Create request) {

        ExpenseResponse created = expenseService.createExpense(request);
        String alert = expenseService.checkMonthlyLimitAlert(securityUtil.getCurrentUserId());

        ApiResponse<ExpenseResponse> response = alert != null
                ? ApiResponse.<ExpenseResponse>builder()
                        .success(true)
                        .message("Expense created successfully")
                        .data(created)
                        .statusCode(201)
                        .alert(alert)
                        .build()
                : ApiResponse.created(created, "Expense created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "[Manager/Admin] Update an expense (blocked if APPROVED)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest.Update request) {
        return ResponseEntity.ok(
                ApiResponse.success(expenseService.updateExpense(id, request),
                        "Expense updated successfully"));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "[Manager/Admin] Delete an expense (blocked if APPROVED)")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    // ── Approve / Reject ──────────────────────────────────────────────────────

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "[Manager/Admin] Approve or reject a PENDING expense")
    public ResponseEntity<ApiResponse<ExpenseResponse>> reviewExpense(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(expenseService.reviewExpense(id, request),
                        "Expense reviewed successfully"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Sort buildSort(String sortBy, String sortDir) {
        return sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
    }
}
