package com.finance.dashboard.security;

import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security {@link UserDetails} adapter wrapping the {@link User} entity.
 *
 * Authority mapping:
 *   Role.EMPLOYEE → "ROLE_EMPLOYEE"
 *   Role.MANAGER  → "ROLE_MANAGER"
 *   Role.ADMIN    → "ROLE_ADMIN"
 *
 * Status mapping used by Spring Security:
 * - {@link #isEnabled()}          → false when status = INACTIVE → throws DisabledException at login
 * - {@link #isAccountNonLocked()} → false when status = INACTIVE → throws LockedException
 *
 * {@link com.finance.dashboard.security.JwtAuthFilter} additionally checks {@code isEnabled()}
 * on every request so that deactivating a user takes effect immediately, even for existing sessions.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Spring Security uses email as the username throughout the application. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Returns false for INACTIVE users to trigger LockedException during authentication. */
    @Override
    public boolean isAccountNonLocked() {
        return UserStatus.ACTIVE.equals(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Returns false for INACTIVE users.
     * Spring Security throws {@link org.springframework.security.authentication.DisabledException}
     * when this returns false during an authentication attempt.
     * {@link JwtAuthFilter} also checks this on every subsequent request.
     */
    @Override
    public boolean isEnabled() {
        return UserStatus.ACTIVE.equals(user.getStatus());
    }

    /** Convenience accessor for the database primary key. */
    public Long getUserId() {
        return user.getId();
    }

    /** Returns the raw role name: "ADMIN", "MANAGER", or "EMPLOYEE". */
    public String getRole() {
        return user.getRole().name();
    }
}
