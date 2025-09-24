package com.pravell.expense.application;

import com.pravell.expense.application.dto.request.CreateExpenseApplicationRequest;
import com.pravell.expense.application.dto.response.ExpenseResponse;
import com.pravell.expense.domain.model.PlanMember;
import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.user.application.UserService;
import com.pravell.user.application.dto.UserMemberDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpenseFacade {

    private final UserService userService;
    private final PlanService planService;
    private final CreateExpenseService createExpenseService;
    private final FindExpenseService findExpenseService;

    public UUID createExpense(UUID userId, UUID planId, CreateExpenseApplicationRequest request) {
        userService.findUserById(userId);
        userService.findUserById(request.getPaidByUserId());

        List<PlanMember> planMembers = getPlanMember(planId);
        return createExpenseService.create(userId, planId, request, planMembers);
    }

    public List<ExpenseResponse> getExpenses(UUID userId, UUID planId, LocalDateTime from, LocalDateTime to,
                                             UUID paidByUserId) {
        userService.findUserById(userId);
        if (paidByUserId != null) {
            userService.findUserById(paidByUserId);
        }

        List<PlanMember> members = getMembers(planId);

        return findExpenseService.findAll(planId, userId, members, from, to, paidByUserId);
    }

    private List<PlanMember> getMembers(UUID planId) {
        List<PlanMember> planMembers = getPlanMember(planId);
        List<UserMemberDTO> userServiceMembers = userService.findMembers(
                planMembers.stream().map(PlanMember::getMemberId).toList());
        return userServiceMembers.stream().map(usm -> {
            return PlanMember.builder()
                    .memberId(usm.getMemberId())
                    .nickname(usm.getNickname())
                    .build();
        }).toList();
    }

    private List<PlanMember> getPlanMember(UUID planId) {
        planService.findPlan(planId);
        return getPlanMembers(planId);
    }

    private List<PlanMember> getPlanMembers(UUID planId) {
        List<PlanMemberDTO> planMembers = planService.findActivePlanMembers(planId);

        return planMembers.stream().map(
                pm -> {
                    return PlanMember.builder()
                            .memberId(pm.getMemberId())
                            .build();
                }
        ).toList();
    }
}
