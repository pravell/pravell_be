package com.pravell.expense.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.expense.application.dto.request.CreateExpenseApplicationRequest;
import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.domain.model.PlanMember;
import com.pravell.expense.domain.repository.ExpenseRepository;
import com.pravell.expense.domain.service.ExpenseAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateExpenseService {

    private final ExpenseAuthorizationService expenseAuthorizationService;
    private final ExpenseRepository expenseRepository;

    public UUID create(UUID userId, UUID planId, CreateExpenseApplicationRequest request,
                       List<PlanMember> planMembers) {
        validateCreateExpense(userId, planMembers);
        return saveExpense(userId, planId, request);
    }

    private UUID saveExpense(UUID userId, UUID planId, CreateExpenseApplicationRequest request) {
        Expense expense = Expense.create(planId, request.getPaidByUserId(), request.getAmount(), request.getTitle(),
                request.getDescription(), request.getSpentAt(), userId);
        expenseRepository.save(expense);
        return expense.getId();
    }

    private void validateCreateExpense(UUID userId, List<PlanMember> planMembers) {
        if (!expenseAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }
    }

}
