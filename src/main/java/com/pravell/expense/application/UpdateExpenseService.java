package com.pravell.expense.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.expense.application.dto.request.UpdateExpenseApplicationRequest;
import com.pravell.expense.application.dto.response.ExpenseResponse;
import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.domain.model.PlanMember;
import com.pravell.expense.domain.service.ExpenseAuthorizationService;
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
public class UpdateExpenseService {

    private final ExpenseAuthorizationService expenseAuthorizationService;

    @Transactional
    public ExpenseResponse update(Expense expense, UpdateExpenseApplicationRequest request, UUID userId,
                                  List<PlanMember> members) {
        log.info("{} 유저가 {} 플랜의 {} 지출을 수정. before = {}, after = {}",
                userId, expense.getPlanId(), expense.getId(), expense.toString(), request.toString());

        validateUpdateExpense(expense, userId, members);
        updateExpense(expense, request, members);

        return getExpenseResponse(expense, members);
    }

    private void validateUpdateExpense(Expense expense, UUID userId, List<PlanMember> members) {
        if (!expenseAuthorizationService.isOwnerOrMember(userId, members)) {
            log.info("{} 유저는 {} 플랜의 {} 지출을 수정 할 권한이 없습니다.", userId, expense.getPlanId(), expense.getId());
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }
    }

    private void updateExpense(Expense expense, UpdateExpenseApplicationRequest request, List<PlanMember> members) {
        if (request.getTitle() != null && !request.getTitle().equals(expense.getTitle())) {
            expense.updateTitle(request.getTitle());
        }
        if (request.getAmount() != null && !request.getAmount().equals(expense.getAmount())) {
            expense.updateAmount(request.getAmount());
        }
        if (request.getPaidByUserId() != null && !request.getPaidByUserId().equals(expense.getPaidByUserId())) {
            if (!expenseAuthorizationService.isOwnerOrMember(request.getPaidByUserId(), members)) {
                log.info("{} 유저는 {} 플랜에 속해있지 않아 {} 지출 내역에 지출한 유저로 등록이 불가능합니다.",
                        request.getPaidByUserId(), expense.getPlanId(), expense.getId());
                throw new AccessDeniedException("해당 유저는 플랜에 속해있지 않아 지출한 유저로 등록이 불가능합니다.");
            }
            expense.updatePaidByUserId(request.getPaidByUserId());
        }
        if (request.getSpentAt() != null && !request.getSpentAt().equals(expense.getSpentAt())) {
            expense.updateSpentAt(request.getSpentAt());
        }
        if (request.getDescription() != null && !request.getDescription().equals(expense.getDescription())) {
            expense.updateDescription(request.getDescription());
        }
    }

    private ExpenseResponse getExpenseResponse(Expense expense, List<PlanMember> planMembers) {
        Map<UUID, String> idToNickname = getUuidStringMap(planMembers);

        return ExpenseResponse.builder()
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
