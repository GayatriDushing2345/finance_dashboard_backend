package com.finance.dashboard;

import com.finance.dashboard.dto.request.ApprovalRequest;
import com.finance.dashboard.dto.request.ExpenseRequest;
import com.finance.dashboard.dto.response.ExpenseResponse;
import com.finance.dashboard.entity.Category;
import com.finance.dashboard.entity.Expense;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.ExpenseStatus;
import com.finance.dashboard.enums.ExpenseType;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.exception.AppException;
import com.finance.dashboard.repository.ExpenseRepository;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.service.impl.CategoryServiceImpl;
import com.finance.dashboard.service.impl.ExpenseServiceImpl;
import com.finance.dashboard.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseServiceImpl Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository   expenseRepository;
    @Mock private UserRepository      userRepository;
    @Mock private CategoryServiceImpl categoryService;
    @Mock private SecurityUtil        securityUtil;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private User     employee;
    private User     manager;
    private Category category;
    private Expense  pendingExpense;
    private Expense  approvedExpense;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(expenseService, "monthlyLimit", new BigDecimal("10000.00"));

        employee = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .role(Role.EMPLOYEE).status(UserStatus.ACTIVE).build();

        manager = User.builder()
                .id(2L).name("Bob").email("bob@test.com")
                .role(Role.MANAGER).status(UserStatus.ACTIVE).build();

        category = Category.builder().id(10L).name("Travel").build();

        pendingExpense = Expense.builder()
                .id(100L).amount(new BigDecimal("500.00"))
                .type(ExpenseType.EXPENSE).category(category)
                .user(employee).date(LocalDate.now())
                .status(ExpenseStatus.PENDING).createdByUser(employee)
                .build();

        approvedExpense = Expense.builder()
                .id(101L).amount(new BigDecimal("300.00"))
                .type(ExpenseType.EXPENSE).category(category)
                .user(employee).date(LocalDate.now())
                .status(ExpenseStatus.APPROVED).createdByUser(employee)
                .build();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createExpense()")
    class CreateExpense {

        @Test
        @DisplayName("creates expense with PENDING status")
        void createExpense_success() {
            ExpenseRequest.Create req = new ExpenseRequest.Create();
            req.setAmount(new BigDecimal("250.00"));
            req.setType(ExpenseType.EXPENSE);
            req.setCategoryId(10L);
            req.setDate(LocalDate.now());

            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(categoryService.findEntityById(10L)).thenReturn(category);
            when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

            ExpenseResponse result = expenseService.createExpense(req);

            assertThat(result.getStatus()).isEqualTo(ExpenseStatus.PENDING);
            assertThat(result.getAmount()).isEqualByComparingTo("250.00");
            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("throws 404 when category does not exist")
        void createExpense_categoryNotFound() {
            ExpenseRequest.Create req = new ExpenseRequest.Create();
            req.setAmount(new BigDecimal("100.00"));
            req.setType(ExpenseType.INCOME);
            req.setCategoryId(999L);
            req.setDate(LocalDate.now());

            when(securityUtil.getCurrentUserId()).thenReturn(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(categoryService.findEntityById(999L))
                    .thenThrow(new AppException.ResourceNotFoundException("Category", 999L));

            assertThatThrownBy(() -> expenseService.createExpense(req))
                    .isInstanceOf(AppException.ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: 999");
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getExpenseById()")
    class GetExpenseById {

        @Test
        @DisplayName("employee can read their own expense")
        void employee_canReadOwnExpense() {
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.isEmployee()).thenReturn(true);
            when(securityUtil.getCurrentUserId()).thenReturn(1L);

            ExpenseResponse result = expenseService.getExpenseById(100L);
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("employee cannot read another user's expense")
        void employee_cannotReadOthersExpense() {
            Expense otherExpense = Expense.builder()
                    .id(200L).user(manager).category(category)
                    .status(ExpenseStatus.PENDING).date(LocalDate.now())
                    .amount(BigDecimal.TEN).type(ExpenseType.EXPENSE)
                    .build();

            when(expenseRepository.findById(200L)).thenReturn(Optional.of(otherExpense));
            when(securityUtil.isEmployee()).thenReturn(true);
            when(securityUtil.getCurrentUserId()).thenReturn(1L);

            assertThatThrownBy(() -> expenseService.getExpenseById(200L))
                    .isInstanceOf(AppException.AccessDeniedException.class)
                    .hasMessageContaining("only view your own expenses");
        }

        @Test
        @DisplayName("manager can read any expense")
        void manager_canReadAnyExpense() {
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.isEmployee()).thenReturn(false);

            ExpenseResponse result = expenseService.getExpenseById(100L);
            assertThat(result.getId()).isEqualTo(100L);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateExpense()")
    class UpdateExpense {

        @Test
        @DisplayName("manager can update a PENDING expense")
        void manager_canUpdatePendingExpense() {
            ExpenseRequest.Update req = new ExpenseRequest.Update();
            req.setAmount(new BigDecimal("750.00"));

            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.isEmployee()).thenReturn(false);
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ExpenseResponse result = expenseService.updateExpense(100L, req);
            assertThat(result.getAmount()).isEqualByComparingTo("750.00");
        }

        @Test
        @DisplayName("blocks update on an APPROVED expense")
        void blocksUpdate_onApprovedExpense() {
            when(expenseRepository.findById(101L)).thenReturn(Optional.of(approvedExpense));
            when(securityUtil.isEmployee()).thenReturn(false);

            assertThatThrownBy(() -> expenseService.updateExpense(101L, new ExpenseRequest.Update()))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .hasMessageContaining("cannot be edited");
        }

        @Test
        @DisplayName("employee cannot update any expense")
        void employee_cannotUpdate() {
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.isEmployee()).thenReturn(true);

            assertThatThrownBy(() -> expenseService.updateExpense(100L, new ExpenseRequest.Update()))
                    .isInstanceOf(AppException.AccessDeniedException.class);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteExpense()")
    class DeleteExpense {

        @Test
        @DisplayName("manager can delete a PENDING expense")
        void manager_canDeletePendingExpense() {
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.isEmployee()).thenReturn(false);

            expenseService.deleteExpense(100L);
            verify(expenseRepository).delete(pendingExpense);
        }

        @Test
        @DisplayName("blocks deletion of an APPROVED expense")
        void blocksDeletion_onApprovedExpense() {
            when(expenseRepository.findById(101L)).thenReturn(Optional.of(approvedExpense));
            when(securityUtil.isEmployee()).thenReturn(false);

            assertThatThrownBy(() -> expenseService.deleteExpense(101L))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .hasMessageContaining("cannot be deleted");
        }
    }

    // ── REVIEW ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reviewExpense()")
    class ReviewExpense {

        @Test
        @DisplayName("manager can approve a PENDING expense")
        void manager_canApprovePending() {
            ApprovalRequest req = new ApprovalRequest();
            req.setStatus(ExpenseStatus.APPROVED);

            when(securityUtil.isManagerOrAdmin()).thenReturn(true);
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.getCurrentUserId()).thenReturn(2L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ExpenseResponse result = expenseService.reviewExpense(100L, req);
            assertThat(result.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        }

        @Test
        @DisplayName("manager can reject a PENDING expense")
        void manager_canRejectPending() {
            ApprovalRequest req = new ApprovalRequest();
            req.setStatus(ExpenseStatus.REJECTED);

            when(securityUtil.isManagerOrAdmin()).thenReturn(true);
            when(expenseRepository.findById(100L)).thenReturn(Optional.of(pendingExpense));
            when(securityUtil.getCurrentUserId()).thenReturn(2L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ExpenseResponse result = expenseService.reviewExpense(100L, req);
            assertThat(result.getStatus()).isEqualTo(ExpenseStatus.REJECTED);
        }

        @Test
        @DisplayName("rejects PENDING as a review outcome")
        void blocks_pendingAsReviewStatus() {
            ApprovalRequest req = new ApprovalRequest();
            req.setStatus(ExpenseStatus.PENDING);

            when(securityUtil.isManagerOrAdmin()).thenReturn(true);

            assertThatThrownBy(() -> expenseService.reviewExpense(100L, req))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .hasMessageContaining("APPROVED or REJECTED");
        }

        @Test
        @DisplayName("employee cannot review expenses")
        void employee_cannotReview() {
            when(securityUtil.isManagerOrAdmin()).thenReturn(false);

            assertThatThrownBy(() -> expenseService.reviewExpense(100L, new ApprovalRequest()))
                    .isInstanceOf(AppException.AccessDeniedException.class);
        }

        @Test
        @DisplayName("cannot review an already-APPROVED expense")
        void cannotReview_alreadyApproved() {
            ApprovalRequest req = new ApprovalRequest();
            req.setStatus(ExpenseStatus.REJECTED);

            when(securityUtil.isManagerOrAdmin()).thenReturn(true);
            when(expenseRepository.findById(101L)).thenReturn(Optional.of(approvedExpense));

            assertThatThrownBy(() -> expenseService.reviewExpense(101L, req))
                    .isInstanceOf(AppException.BadRequestException.class)
                    .hasMessageContaining("Only PENDING expenses can be reviewed");
        }
    }

    // ── MONTHLY LIMIT ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("checkMonthlyLimitAlert()")
    class MonthlyLimitAlert {

        @Test
        @DisplayName("returns alert message when spending exceeds limit")
        void returnsAlert_whenExceeded() {
            when(expenseRepository.sumMonthlyExpensesByUser(anyInt(), anyInt(), eq(1L)))
                    .thenReturn(new BigDecimal("15000.00"));

            String alert = expenseService.checkMonthlyLimitAlert(1L);
            assertThat(alert).isNotNull().contains("exceeded");
        }

        @Test
        @DisplayName("returns null when spending is within limit")
        void returnsNull_withinLimit() {
            when(expenseRepository.sumMonthlyExpensesByUser(anyInt(), anyInt(), eq(1L)))
                    .thenReturn(new BigDecimal("5000.00"));

            assertThat(expenseService.checkMonthlyLimitAlert(1L)).isNull();
        }

        @Test
        @DisplayName("returns null when no spending recorded (null from DB)")
        void returnsNull_whenNull() {
            when(expenseRepository.sumMonthlyExpensesByUser(anyInt(), anyInt(), eq(1L)))
                    .thenReturn(null);

            assertThat(expenseService.checkMonthlyLimitAlert(1L)).isNull();
        }
    }
}
