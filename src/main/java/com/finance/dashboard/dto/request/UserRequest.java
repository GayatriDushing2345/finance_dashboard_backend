package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTOs for user management endpoints (Admin-only except where noted).
 */
public class UserRequest {

    /** Used by Admin for {@code POST /api/admin/users}. */
    @Data
    public static class Create {

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        private String password;

        @NotNull(message = "Role is required (EMPLOYEE, MANAGER, or ADMIN)")
        private Role role;
    }

    /** Used by Admin for {@code PATCH /api/admin/users/{id}/role}. */
    @Data
    public static class UpdateRole {

        @NotNull(message = "Role is required (EMPLOYEE, MANAGER, or ADMIN)")
        private Role role;
    }

    /** Used by Admin for {@code PATCH /api/admin/users/{id}/status}. */
    @Data
    public static class UpdateStatus {

        @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
        private UserStatus status;
    }
}
