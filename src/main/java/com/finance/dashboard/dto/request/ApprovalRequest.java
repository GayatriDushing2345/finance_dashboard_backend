package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.ExpenseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Used for (MANAGER / ADMIN only).
 * Status must be APPROVED or REJECTED – PENDING is rejected in service validation.
 */
@Data
public class ApprovalRequest {

    @NotNull(message = "Status is required (APPROVED or REJECTED)")
    private ExpenseStatus status;
}
