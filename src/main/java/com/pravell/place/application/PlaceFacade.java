package com.pravell.place.application;

import com.pravell.place.application.dto.request.SavePlaceApplicationRequest;
import com.pravell.place.application.dto.response.SavePlaceResponse;
import com.pravell.place.domain.model.PlanMember;
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

    private List<PlanMember> getPlanMembers(UUID planId) {
        List<PlanMemberDTO> planMembers = planService.findPlanMembers(planId);

        return planMembers.stream().map(
                pm -> {
                    return PlanMember.builder()
                            .memberId(pm.getMemberId())
                            .planMemberStatus(pm.getPlanMemberStatus())
                            .build();
                }
        ).toList();
    }

}
