package com.pravell.plan.application;

import com.pravell.plan.application.dto.request.KickUsersFromPlanApplicationRequest;
import com.pravell.plan.application.dto.request.WithdrawFromPlansApplicationRequest;
import com.pravell.plan.application.dto.response.InviteCodeResponse;
import com.pravell.plan.application.dto.response.PlanJoinUserResponse;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanInviteCode;
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
    private final JoinPlanService joinPlanService;
    private final WithdrawPlanService withdrawPlanService;
    private final KickUserService kickUserService;

    public InviteCodeResponse createInviteCode(UUID planId, UUID userId) {
        userService.findUserById(userId);

        Plan plan = planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        String code = createInviteCodeService.create(plan, planUsers, userId);
        return new InviteCodeResponse(code);
    }

    public PlanJoinUserResponse join(UUID userId, String code) {
        userService.findUserById(userId);
        PlanInviteCode planInviteCode = joinPlanService.findPlanInviteCode(code);
        Plan plan = planService.findPlan(planInviteCode.getPlanId());

        joinPlanService.join(userId, plan);

        return new PlanJoinUserResponse(plan.getId());
    }

    public void withdrawPlans(UUID id, WithdrawFromPlansApplicationRequest applicationRequest) {
        userService.findUserById(id);

        withdrawPlanService.withdrawFromPlans(id, applicationRequest);
    }

    public void kickUsers(UUID id, UUID planId, KickUsersFromPlanApplicationRequest request) {
        userService.findUserById(id);

        planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        kickUserService.kickUsers(id, planUsers, request);
    }

}
