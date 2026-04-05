package com.finance.dashboard.enums;

/**
 * Indicates whether a user account is allowed to authenticate.
 * INACTIVE users are rejected at login and blocked mid-session by JwtAuthFilter.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}
