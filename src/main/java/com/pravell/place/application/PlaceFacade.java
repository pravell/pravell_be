package com.pravell.place.application;

import com.pravell.place.application.dto.request.SavePlaceApplicationRequest;
import com.pravell.place.application.dto.response.FindPlanPlacesResponse;
import com.pravell.place.application.dto.response.SavePlaceResponse;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.model.PlanMemberStatus;
import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceFacade {

    private final UserService userService;
    private final SavePlaceService savePlaceService;
    private final PlanService planService;
    private final FindPlaceService findPlaceService;

    public SavePlaceResponse savePlace(UUID id, SavePlaceApplicationRequest request) {
        userService.findUserById(id);

        log.info("{} 유저가 {} 플랜에 {} 장소 저장", id, request.getPlanId(), request.getTitle());

        planService.findPlan(request.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(request.getPlanId());

        Long pinPlaceId = savePlaceService.save(id, request, planMembers);

        return SavePlaceResponse.builder()
                .pinPlaceId(pinPlaceId)
                .build();
    }

    public List<FindPlanPlacesResponse> findPlanPlaces(UUID id, UUID planId) {
        userService.findUserById(id);

        boolean isPlanPublic = planService.isPlanPublic(planId);
        List<PlanMember> planMembers = getPlanMembers(planId);

        return findPlaceService.findAll(id, planId, planMembers, isPlanPublic);
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
