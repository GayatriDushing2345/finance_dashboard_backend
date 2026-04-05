package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.AuthRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.AuthResponse;
import com.finance.dashboard.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints – no JWT token required.
 *
 * All successful responses include a Bearer JWT token that must be
 * included in subsequent secured requests:
 *   Authorization: Bearer {@literal <token>}
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and self-registration (no token required)")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticate with email and password.
     * Returns a JWT token valid for the duration configured in {@code app.jwt.expiration}.
     * Returns 403 if the account is INACTIVE.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password – returns JWT Bearer token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(
                ApiResponse.success(authService.login(request), "Login successful"));
    }

    /**
     * Self-register a new EMPLOYEE account.
     * Returns a JWT token immediately – no separate login step needed.
     * Returns 409 if the email is already registered.
     */
    @PostMapping("/register")
    @Operation(summary = "Self-register as EMPLOYEE (no admin action needed)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.register(request), "Registration successful"));
    }
}
