package com.finance.dashboard.util;

import com.finance.dashboard.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility for accessing the currently authenticated user's details from anywhere
 * in the service layer without coupling to HTTP request objects.
 *
 * The {@link SecurityContextHolder} is populated by {@link com.finance.dashboard.security.JwtAuthFilter}
 * on every authenticated request and is available for the lifetime of that request thread.
 *
 * Usage (inject and call):
 * <pre>{@code
 *   Long currentUserId = securityUtil.getCurrentUserId();
 *   if (securityUtil.isEmployee()) { ... }
 * }</pre>
 */
@Component
public class SecurityUtil {

    /**
     * Returns the {@link UserPrincipal} of the currently authenticated user.
     *
     * @throws IllegalStateException if called outside a secured request context
     *         (should never happen for any endpoint behind Spring Security).
     */
    public UserPrincipal getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new IllegalStateException(
                "No authenticated user found in SecurityContext. " +
                "This method must only be called within a secured request.");
    }

    /** Returns the database primary key of the authenticated user. */
    public Long getCurrentUserId() {
        return getCurrentUserPrincipal().getUserId();
    }

    /** Returns the raw role name: "ADMIN", "MANAGER", or "EMPLOYEE". */
    public String getCurrentUserRole() {
        return getCurrentUserPrincipal().getRole();
    }

    /** {@code true} if the authenticated user has the ADMIN role. */
    public boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    /** {@code true} if the authenticated user is MANAGER or ADMIN. */
    public boolean isManagerOrAdmin() {
        String role = getCurrentUserRole();
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }

    /** {@code true} if the authenticated user is an EMPLOYEE (lowest privilege). */
    public boolean isEmployee() {
        return "EMPLOYEE".equals(getCurrentUserRole());
    }
}
