package com.finance.dashboard.enums;

/**
 * Lifecycle states of an expense record.

 * State machine:
 *   PENDING ──► APPROVED   (by MANAGER or ADMIN)
 *   PENDING ──► REJECTED   (by MANAGER or ADMIN)
 * APPROVED expenses are immutable – edits and deletes are blocked.
 */
public enum ExpenseStatus {
    PENDING,
    APPROVED,
    REJECTED
}
