package com.finance.dashboard.dto.response;

import com.finance.dashboard.entity.Expense;
import com.finance.dashboard.enums.ExpenseStatus;
import com.finance.dashboard.enums.ExpenseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Outbound expense representation.
 *
 * Includes audit fields (createdBy, reviewedBy, timestamps) so the client
 * can display a full audit trail without additional API calls.
 * Lazy-loaded relations are accessed inside a transaction in the service layer
 * before this DTO is constructed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private Long          id;
    private BigDecimal    amount;
    private ExpenseType   type;
    private Long          categoryId;
    private String        categoryName;
    private Long          userId;
    private String        userName;
    private LocalDate     date;
    private String        note;
    private ExpenseStatus status;

    // Audit trail fields
    private Long          createdById;
    private String        createdByName;
    private Long          reviewedById;
    private String        reviewedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpenseResponse from(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .type(expense.getType())
                .categoryId(expense.getCategory() != null ? expense.getCategory().getId() : null)
                .categoryName(expense.getCategory() != null ? expense.getCategory().getName() : null)
                .userId(expense.getUser() != null ? expense.getUser().getId() : null)
                .userName(expense.getUser() != null ? expense.getUser().getName() : null)
                .date(expense.getDate())
                .note(expense.getNote())
                .status(expense.getStatus())
                .createdById(expense.getCreatedByUser() != null ? expense.getCreatedByUser().getId() : null)
                .createdByName(expense.getCreatedByUser() != null ? expense.getCreatedByUser().getName() : null)
                .reviewedById(expense.getReviewedByUser() != null ? expense.getReviewedByUser().getId() : null)
                .reviewedByName(expense.getReviewedByUser() != null ? expense.getReviewedByUser().getName() : null)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
