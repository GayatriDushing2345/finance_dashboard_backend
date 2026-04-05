package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.request.UserRequest;
import com.finance.dashboard.dto.response.UserResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.exception.AppException;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.service.UserService;
import com.finance.dashboard.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management service (Admin-only operations, except {@link #getCurrentUser()}).
 *
 * Safety guards:
 * - Admin cannot change their own role → prevents accidental self-demotion.
 * - Admin cannot deactivate their own account → prevents lockout.
 * - Admin cannot delete their own account → prevents accidental self-deletion.
 *
 * All mutations are logged at INFO level for audit purposes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil    securityUtil;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserResponse.from(findById(securityUtil.getCurrentUserId()));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest.Create request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException.ConflictException(
                    "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        log.info("Admin created user: {} with role: {}", user.getEmail(), user.getRole());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.from(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long userId, UserRequest.UpdateRole request) {
        guardSelfModification(userId, "change your own role");
        User user = findById(userId);
        log.info("Admin changed role for user {} from {} to {}", user.getEmail(), user.getRole(), request.getRole());
        user.setRole(request.getRole());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long userId, UserRequest.UpdateStatus request) {
        guardSelfModification(userId, "change your own account status");
        User user = findById(userId);
        log.info("Admin changed status for user {} from {} to {}", user.getEmail(), user.getStatus(), request.getStatus());
        user.setStatus(request.getStatus());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        guardSelfModification(userId, "delete your own account");
        User user = findById(userId);
        log.info("Admin deleted user: {}", user.getEmail());
        userRepository.delete(user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException.ResourceNotFoundException("User", id));
    }

    /**
     * Prevents an Admin from modifying their own account in destructive ways.
     * The error message is contextualised with what action was attempted.
     */
    private void guardSelfModification(Long targetUserId, String action) {
        if (targetUserId.equals(securityUtil.getCurrentUserId())) {
            throw new AppException.BadRequestException("You cannot " + action + ".");
        }
    }
}
