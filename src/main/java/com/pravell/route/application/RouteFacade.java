package com.pravell.route.application;

import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.route.application.dto.request.CreateRouteApplicationRequest;
import com.pravell.route.application.dto.response.CreateRouteResponse;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.PlanMemberStatus;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteFacade {

    private final UserService userService;
    private final PlanService planService;
    private final CreateRouteService createRouteService;

    public CreateRouteResponse createRoute(UUID userId, CreateRouteApplicationRequest request) {
        validateUserAndPlan(userId, request);

        List<PlanMember> planMembers = getPlanMembers(request.getPlanId());

        return createRouteService.create(userId, request, planMembers);
    }

    private void validateUserAndPlan(UUID userId, CreateRouteApplicationRequest request) {
        userService.findUserById(userId);
        planService.findPlan(request.getPlanId());
    }

    private List<PlanMember> getPlanMembers(UUID planId) {
        List<PlanMemberDTO> planMembers = planService.findPlanMembers(planId);

        return planMembers.stream().map(
                pm -> {
                    return PlanMember.builder()
                            .memberId(pm.getMemberId())
                            .planMemberStatus(PlanMemberStatus.valueOf(pm.getPlanMemberStatus()))
                            .build();
                }
        ).toList();
    }
}
