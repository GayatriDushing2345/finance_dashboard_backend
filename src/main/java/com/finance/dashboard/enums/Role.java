package com.finance.dashboard.enums;

/**
 * User roles ordered by increasing privilege.
 * Used by Spring Security's {@code hasRole()} and {@code @PreAuthorize} checks.

 */
public enum Role {
    EMPLOYEE,
    MANAGER,
    ADMIN
}
