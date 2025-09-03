package com.pravell.plan.application;

import com.pravell.plan.application.dto.response.InviteCodeResponse;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanMemberFacade {

    private final UserService userService;
    private final PlanService planService;
    private final CreateInviteCodeService createInviteCodeService;

    public InviteCodeResponse createInviteCode(UUID planId, UUID userId) {
        userService.findUserById(userId);

        Plan plan = planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        String code = createInviteCodeService.create(plan, planUsers, userId);
        return new InviteCodeResponse(code);
    }
}
