package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.ExpenseType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTOs for expense operations.
 *
 * Design decisions:
 * - Annotation-based validation handles field-level constraints (null, range, size).
 * - Business-rule validation (categoryId existence, date ordering, APPROVED immutability)
 *   is handled in {@code ExpenseServiceImpl} to keep annotations focused on structural checks.
 * - The Update DTO uses nullable fields for partial updates (PATCH semantics):
 *   only non-null fields are applied to the existing entity.
 */
public class ExpenseRequest {

    @Data
    public static class Create {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits and 2 decimal places")
        private BigDecimal amount;

        @NotNull(message = "Type is required (INCOME or EXPENSE)")
        private ExpenseType type;

        @NotNull(message = "Category ID is required")
        @Positive(message = "Category ID must be a positive number")
        private Long categoryId;

        @NotNull(message = "Date is required")
        private LocalDate date;

        @Size(max = 500, message = "Note must not exceed 500 characters")
        private String note;
    }

    /**
     * Used for {@code PUT /api/expenses/{id}}.
     * All fields are optional – only non-null values are applied to the stored record.
     * Blocked entirely if the expense status is APPROVED.
     */
    @Data
    public static class Update {

        @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 digits and 2 decimal places")
        private BigDecimal amount;

        private ExpenseType type;

        @Positive(message = "Category ID must be a positive number")
        private Long categoryId;

        private LocalDate date;

        @Size(max = 500, message = "Note must not exceed 500 characters")
        private String note;
    }

    /**
     * Query parameters for {@code GET /api/expenses}.
     * All fields are optional; null means "no filter applied for this dimension".
     */
    @Data
    public static class Filter {
        private Long        categoryId;
        private ExpenseType type;
        private LocalDate   startDate;
        private LocalDate   endDate;
        private String      status;    // "PENDING" | "APPROVED" | "REJECTED" – validated in service
        private Integer     page    = 0;
        private Integer     size    = 10;
        private String      sortBy  = "createdAt";
        private String      sortDir = "desc";
    }
}
