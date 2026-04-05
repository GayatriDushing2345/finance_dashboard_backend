package com.finance.dashboard.entity;

import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an application user.

 * Account status:
 * - ACTIVE  → can log in, holds valid sessions
 * - INACTIVE → blocked at login and mid-session (JwtAuthFilter checks on every request)
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ──────────────────────────────────────────────────────────

    /** Expenses belonging to this user (the primary owner). */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Expense> expenses = new ArrayList<>();

    /** Expenses this user submitted (audit trail for who created the record). */
    @OneToMany(mappedBy = "createdByUser", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Expense> createdExpenses = new ArrayList<>();

    /** Expenses this user approved or rejected (Manager/Admin audit trail). */
    @OneToMany(mappedBy = "reviewedByUser", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Expense> reviewedExpenses = new ArrayList<>();

    // ── Domain helpers ─────────────────────────────────────────────────────────

    /** Returns true if the account is allowed to authenticate. */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
}
