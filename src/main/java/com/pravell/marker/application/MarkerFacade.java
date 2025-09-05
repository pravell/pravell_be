package com.pravell.marker.application;

import com.pravell.marker.application.dto.request.CreateMarkerApplicationRequest;
import com.pravell.marker.application.dto.response.CreateMarkerResponse;
import com.pravell.marker.domain.model.PlanMember;
import com.pravell.marker.domain.model.PlanMemberStatus;
import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarkerFacade {

    private final UserService userService;
    private final PlanService planService;
    private final CreateMarkerService createMarkerService;

    public CreateMarkerResponse createMarker(UUID id, CreateMarkerApplicationRequest request) {
        userService.findUserById(id);

        planService.findPlan(request.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(request.getPlanId());

        return createMarkerService.create(id, planMembers, request);
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
