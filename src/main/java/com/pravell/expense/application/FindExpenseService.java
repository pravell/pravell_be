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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseAuthorizationService expenseAuthorizationService;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll(UUID planId, UUID userId, List<PlanMember> planMembers,
                                         LocalDateTime from, LocalDateTime to, UUID paidByUserId) {
        log.info("{} 유저가 {} 플랜의 지출 내역을 조회.", userId, planId);
        validateFindExpenses(userId, planMembers, planId);
        List<Expense> expenses = expenseRepository.findAllByPlanIdWithFilters(planId, from, to, paidByUserId);
        return getExpenseResponses(expenses, planMembers);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse find(Expense expense, List<PlanMember> planMembers, UUID userId) {
        log.info("{} 유저가 {} 플랜의 {} 지출 내역을 조회.", userId, expense.getPlanId(), expense.getId());
        validateFindExpenses(userId, planMembers, expense.getPlanId());
        return getExpenseResponse(expense, planMembers);
    }

    private void validateFindExpenses(UUID userId, List<PlanMember> planMembers, UUID planId) {
        if (!expenseAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("{} 유저는 {} 플랜의 지출을 조회하지 못합니다.", userId, planId);
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }
    }

    private List<ExpenseResponse> getExpenseResponses(List<Expense> expenses, List<PlanMember> planMembers) {
        Map<UUID, String> idToNickname = getUuidStringMap(planMembers);

        return expenses.stream()
                .map(e -> ExpenseResponse.builder()
                        .expenseId(e.getId())
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

    private ExpenseResponse getExpenseResponse(Expense expense, List<PlanMember> planMembers) {
        Map<UUID, String> idToNickname = getUuidStringMap(planMembers);

        return ExpenseResponse.builder()
                .expenseId(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .paidByUserId(expense.getPaidByUserId())
                .spentAt(expense.getSpentAt())
                .description(expense.getDescription())
                .paidByUserNickname(idToNickname.getOrDefault(expense.getPaidByUserId(), "플랜에 속하지 않는 유저"))
                .build();
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
