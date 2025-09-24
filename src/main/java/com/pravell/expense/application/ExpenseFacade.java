package com.pravell.expense.application;

import com.pravell.expense.application.dto.request.CreateExpenseApplicationRequest;
import com.pravell.expense.domain.model.PlanMember;
import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.user.application.UserService;
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

    public UUID createExpense(UUID userId, UUID planId, CreateExpenseApplicationRequest request) {
        userService.findUserById(userId);
        userService.findUserById(request.getPaidByUserId());

        List<PlanMember> planMembers = getPlanMember(planId);
        return createExpenseService.create(userId, planId, request, planMembers);
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
