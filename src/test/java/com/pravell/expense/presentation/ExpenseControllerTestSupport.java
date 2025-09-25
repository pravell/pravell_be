package com.pravell.expense.presentation;

import com.pravell.ControllerTestSupport;
import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.domain.repository.ExpenseRepository;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ExpenseControllerTestSupport extends ControllerTestSupport {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PlanRepository planRepository;

    @Autowired
    protected PlanUsersRepository planUsersRepository;

    @Autowired
    protected ExpenseRepository expenseRepository;

    protected User getUser(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .userId("userId" + UUID.randomUUID())
                .nickname("nickname" + UUID.randomUUID())
                .password("passworddd")
                .status(status)
                .build();
    }

    protected Plan getPlan(boolean isDeleted) {
        return getPlan(isDeleted, true);
    }

    protected Plan getPlan(boolean isDeleted, boolean isPublic) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("name")
                .isDeleted(isDeleted)
                .isPublic(isPublic)
                .startDate(LocalDate.parse("2025-09-29"))
                .endDate(LocalDate.parse("2025-09-30"))
                .build();
    }

    protected PlanUsers getPlanUsers(UUID planId, UUID userId, PlanUserStatus status) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

    protected Expense getExpense(UUID planId, UUID createdBy, UUID paidByUserId, boolean isDeleted) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .title("title")
                .description("description")
                .amount(10000L)
                .spentAt(LocalDateTime.now())
                .planId(planId)
                .createdBy(createdBy)
                .paidByUserId(paidByUserId)
                .isDeleted(isDeleted)
                .build();
    }

    protected Expense getExpense(UUID planId, UUID createdBy, UUID paidByUserId) {
        return getExpense("title", "description", planId, createdBy, paidByUserId, LocalDateTime.now());
    }

    protected Expense getExpense(String title, String description, UUID planId, UUID createdBy, UUID paidByUserId) {
        return getExpense(title, description, planId, createdBy, paidByUserId, LocalDateTime.now());
    }

    protected Expense getExpense(String title, String description, UUID planId, UUID createdBy, UUID paidByUserId,
                                 LocalDateTime spentAt) {
        return getExpense(title, description, planId, createdBy, paidByUserId, spentAt, false);
    }

    protected Expense getExpense(String title, String description, UUID planId, UUID createdBy, UUID paidByUserId,
                                 LocalDateTime spentAt, boolean isDeleted) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .title(title)
                .description(description)
                .amount(10000L)
                .spentAt(spentAt)
                .planId(planId)
                .createdBy(createdBy)
                .paidByUserId(paidByUserId)
                .isDeleted(isDeleted)
                .build();
    }

}
