package com.finance.dashboard.service.impl;

import com.finance.dashboard.dto.request.AuthRequest;
import com.finance.dashboard.dto.response.AuthResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.exception.AppException;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.JwtUtil;
import com.finance.dashboard.security.UserPrincipal;
import com.finance.dashboard.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service: login and self-registration.
 *
 * Login flow:
 *  1. Delegate credential verification to {@link AuthenticationManager}.
 *     Spring Security handles BadCredentialsException, DisabledException, LockedException.
 *  2. After successful authentication, generate a JWT token.
 *  3. Return the token along with basic user metadata.
 *
 * Registration:
 *  1. Validate email uniqueness.
 *  2. Persist a new EMPLOYEE user with an encoded password.
 *  3. Return a JWT token immediately – no separate login step required.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;

    @Override
    public AuthResponse login(AuthRequest.Login request) {
        // Throws BadCredentialsException / DisabledException / LockedException on failure
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        // Secondary guard: isEnabled() is also checked inside AuthenticationManager,
        // but this explicit check adds clarity to code readers and provides defence-in-depth.
        if (!principal.isEnabled()) {
            throw new AppException.AccessDeniedException(
                    "Your account is inactive. Please contact an administrator.");
        }

        String token = jwtUtil.generateToken(principal);
        log.info("User logged in: {} (role: {})", principal.getUsername(), principal.getRole());

        return buildAuthResponse(token, principal.getUser());
    }

    @Override
    @Transactional
    public AuthResponse register(AuthRequest.Register request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException.ConflictException(
                    "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.EMPLOYEE)      // self-registration always creates EMPLOYEE
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} (role: EMPLOYEE)", user.getEmail());

        UserPrincipal principal = new UserPrincipal(user);
        return buildAuthResponse(jwtUtil.generateToken(principal), user);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
