package com.pravell.expense.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.expense.application.dto.response.ExpenseResponse;
import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.domain.model.PlanMember;
import com.pravell.expense.domain.repository.ExpenseRepository;
import com.pravell.expense.domain.service.ExpenseAuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseAuthorizationService expenseAuthorizationService;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll(UUID planId, UUID userId, List<PlanMember> planMembers,
                                         LocalDateTime from, LocalDateTime to, UUID paidByUserId) {
        validateFindExpenses(userId, planMembers);
        List<Expense> expenses = expenseRepository.findAllByPlanIdWithFilters(planId, from, to, paidByUserId);
        return getExpenseResponses(expenses, planMembers);
    }

    private void validateFindExpenses(UUID userId, List<PlanMember> planMembers) {
        if (!expenseAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }
    }

    private List<ExpenseResponse> getExpenseResponses(List<Expense> expenses, List<PlanMember> planMembers) {
        Map<UUID, String> idToNickname = getUuidStringMap(planMembers);

        return expenses.stream()
                .map(e -> ExpenseResponse.builder()
                        .title(e.getTitle())
                        .amount(e.getAmount())
                        .paidByUserId(e.getPaidByUserId())
                        .paidByUserNickname(idToNickname.getOrDefault(
                                e.getPaidByUserId(), "플랜에 속하지 않는 유저"))
                        .spentAt(e.getSpentAt())
                        .description(e.getDescription())
                        .build())
                .toList();
    }

    private Map<UUID, String> getUuidStringMap(List<PlanMember> planMembers) {
        return planMembers.stream()
                .filter(pm -> pm.getMemberId() != null)
                .collect(Collectors.toMap(
                        PlanMember::getMemberId,
                        pm -> pm.getNickname() == null ? "" : pm.getNickname(),
                        (oldV, newV) -> oldV
                ));
    }

}
