package com.finance.dashboard.entity;

import com.finance.dashboard.enums.ExpenseStatus;
import com.finance.dashboard.enums.ExpenseType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core financial record representing a single income or expense transaction.

 * Lifecycle:
 *   Created by any user → status=PENDING
 *   Reviewed by MANAGER/ADMIN → status=APPROVED or REJECTED
 *   Once APPROVED → record is immutable (no edits, no deletes)
 */
@Entity
@Table(name = "expenses")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Transaction amount; must be > 0. Precision supports up to 999 trillion. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Whether this record represents money received (INCOME) or spent (EXPENSE). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType type;

    /** Category assigned to this expense for grouping and analytics. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** The user this expense belongs to (primary owner for scoping). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Calendar date on which the transaction occurred. */
    @Column(nullable = false)
    private LocalDate date;

    /** Optional freeform note or description (max 500 chars). */
    @Column(length = 500)
    private String note;

    /** Current workflow state: PENDING → APPROVED | REJECTED. Defaults to PENDING on creation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.PENDING;

    /** Audit: the user who created this record (may differ from owner if admin creates on behalf). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdByUser;

    /** Audit: the MANAGER or ADMIN who approved or rejected this record (null until reviewed). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedByUser;

    /** Auto-set on insert by {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}. */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Auto-updated on every save by {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Domain helpers ──────────────────────────────────────────────────────

    /** Returns true if this expense has been approved (immutable state). */
    public boolean isApproved() {
        return ExpenseStatus.APPROVED.equals(this.status);
    }

    /** Returns true if this expense is still awaiting review. */
    public boolean isPending() {
        return ExpenseStatus.PENDING.equals(this.status);
    }
}
