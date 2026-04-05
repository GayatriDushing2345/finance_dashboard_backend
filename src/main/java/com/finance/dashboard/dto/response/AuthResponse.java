package com.finance.dashboard.dto.response;

import com.finance.dashboard.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned on successful login or registration.
 * Contains the JWT token and enough user metadata so the client
 * can display a personalised UI without an extra /me call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT bearer token – include in subsequent requests as: Authorization: Bearer <accessToken> */
    private String accessToken;

    /** Always "Bearer". */
    private String tokenType;

    private Long   userId;
    private String name;
    private String email;
    private Role   role;
}
