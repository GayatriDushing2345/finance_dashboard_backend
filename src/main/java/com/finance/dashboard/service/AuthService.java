package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.AuthRequest;
import com.finance.dashboard.dto.response.AuthResponse;

/**
 * Contract for authentication operations (login and self-registration).
 * Implementation: {@link com.finance.dashboard.service.impl.AuthServiceImpl}
 */
public interface AuthService {

    /**
     * Authenticates a user by email and password.
     * Returns a JWT token on success.
     * Throws {@link org.springframework.security.authentication.BadCredentialsException} for wrong credentials.
     * Throws {@link org.springframework.security.authentication.DisabledException} for inactive accounts.
     */
    AuthResponse login(AuthRequest.Login request);

    /**
     * Registers a new user as EMPLOYEE and returns a JWT token immediately.
     * Throws {@link com.finance.dashboard.exception.AppException.ConflictException} if email is taken.
     */
    AuthResponse register(AuthRequest.Register request);
}
