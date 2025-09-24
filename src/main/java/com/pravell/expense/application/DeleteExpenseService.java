package com.pravell.expense.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.domain.model.PlanMember;
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
public class DeleteExpenseService {

    private final ExpenseAuthorizationService expenseAuthorizationService;

    @Transactional
    public void delete(Expense expense, UUID userId, List<PlanMember> planMembers) {
        log.info("{} 유저가 {} 플랜의 {} 지출을 삭제.", userId, expense.getPlanId(), expense.getId());
        validateExpenseDeletion(userId, planMembers);
        expense.delete();
    }

    private void validateExpenseDeletion(UUID userId, List<PlanMember> planMembers) {
        if (!expenseAuthorizationService.isOwnerOrMember(userId, planMembers)){
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }
    }

}
