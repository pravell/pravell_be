package com.pravell.marker.application;

import com.pravell.marker.application.dto.request.CreateMarkerApplicationRequest;
import com.pravell.marker.application.dto.request.UpdateMarkerApplicationRequest;
import com.pravell.marker.application.dto.response.MarkerResponse;
import com.pravell.marker.application.dto.response.FindMarkersResponse;
import com.pravell.marker.domain.model.Marker;
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
    private final FindMarkerService findMarkerService;
    private final UpdateMarkerService updateMarkerService;
    private final MarkerService markerService;

    public MarkerResponse createMarker(UUID id, CreateMarkerApplicationRequest request) {
        userService.findUserById(id);

        planService.findPlan(request.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(request.getPlanId());

        return createMarkerService.create(id, planMembers, request);
    }

    public List<FindMarkersResponse> findMarkers(UUID id, UUID planId) {
        userService.findUserById(id);

        boolean planPublic = planService.isPlanPublic(planId);
        List<PlanMember> planMembers = getPlanMembers(planId);

        return findMarkerService.getMarkersOfPlan(id, planPublic, planMembers, planId);
    }

    public MarkerResponse updateMarker(UUID id, Long markerId, UpdateMarkerApplicationRequest request) {
        userService.findUserById(id);

        Marker marker = markerService.findMarker(markerId);
        planService.findPlan(marker.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(marker.getPlanId());

        return updateMarkerService.update(marker, id, planMembers, request);
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
