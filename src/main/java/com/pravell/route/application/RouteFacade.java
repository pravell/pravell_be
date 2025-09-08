package com.pravell.route.application;

import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.route.application.dto.request.CreateRouteApplicationRequest;
import com.pravell.route.application.dto.request.DeleteRouteApplicationRequest;
import com.pravell.route.application.dto.response.CreateRouteResponse;
import com.pravell.route.application.dto.response.FindRoutesResponse;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.PlanMemberStatus;
import com.pravell.route.domain.model.Route;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteFacade {

    private final UserService userService;
    private final PlanService planService;
    private final CreateRouteService createRouteService;
    private final FindRouteService findRouteService;
    private final RouteService routeService;
    private final DeleteRouteService deleteRouteService;

    public CreateRouteResponse createRoute(UUID userId, CreateRouteApplicationRequest request) {
        validateUserAndPlan(userId, request.getPlanId());

        List<PlanMember> planMembers = getPlanMembers(request.getPlanId());

        return createRouteService.create(userId, request, planMembers);
    }

    public List<FindRoutesResponse> findRoutes(UUID userId, UUID planId) {
        userService.findUserById(userId);

        boolean isPublic = planService.isPlanPublic(planId);
        List<PlanMember> planMembers = getPlanMembers(planId);

        return findRouteService.findAll(userId, planId, planMembers, isPublic);
    }

    @Transactional
    public void deleteRoutes(UUID userId, DeleteRouteApplicationRequest request) {
        userService.findUserById(userId);

        request.getRouteId().forEach(routeId -> {
            Route route = routeService.findById(routeId);
            planService.findPlan(route.getPlanId());
            List<PlanMember> planMembers = getPlanMembers(route.getPlanId());

            deleteRouteService.delete(route, userId, planMembers);
        });
    }

    private void validateUserAndPlan(UUID userId, UUID planId) {
        userService.findUserById(userId);
        planService.findPlan(planId);
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
