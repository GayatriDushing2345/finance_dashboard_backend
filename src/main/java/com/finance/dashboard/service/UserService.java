package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.UserRequest;
import com.finance.dashboard.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for user management operations.
 *
 * Access rules (enforced in implementation):
 * - {@link #getCurrentUser()} – any authenticated user
 * - All other methods – ADMIN only
 *
 * Implementation: {@link com.finance.dashboard.service.impl.UserServiceImpl}
 */
public interface UserService {

    /** Returns the profile of the currently authenticated user. */
    UserResponse getCurrentUser();

    /** Creates a user with the specified role (Admin only). */
    UserResponse createUser(UserRequest.Create request);

    /** Returns a user by ID. */
    UserResponse getUserById(Long id);

    /** Returns a paginated list of all users. */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Changes a user's role.
     * Admins cannot change their own role (prevents accidental self-demotion).
     */
    UserResponse updateRole(Long userId, UserRequest.UpdateRole request);

    /**
     * Activates or deactivates a user account.
     * Admins cannot deactivate their own account (prevents lockout).
     */
    UserResponse updateStatus(Long userId, UserRequest.UpdateStatus request);

    /**
     * Permanently deletes a user.
     * Admins cannot delete their own account.
     */
    void deleteUser(Long userId);
}
