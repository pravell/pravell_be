package com.pravell.place.application;

import com.pravell.place.application.dto.request.DeletePlacesApplicationRequest;
import com.pravell.place.application.dto.request.SavePlaceApplicationRequest;
import com.pravell.place.application.dto.request.UpdatePlaceApplicationRequest;
import com.pravell.place.application.dto.response.FindPlanPlacesResponse;
import com.pravell.place.application.dto.response.PlaceResponse;
import com.pravell.place.application.dto.response.SavePlaceResponse;
import com.pravell.place.domain.model.PinPlace;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceFacade {

    private final UserService userService;
    private final SavePlaceService savePlaceService;
    private final PlanService planService;
    private final FindPlaceService findPlaceService;
    private final UpdatePlaceService updatePlaceService;
    private final PlaceService placeService;
    private final DeletePlaceService deletePlaceService;

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

    public PlaceResponse updatePlan(UUID id, Long placeId, UpdatePlaceApplicationRequest request) {
        userService.findUserById(id);

        PinPlace place = placeService.findPlace(placeId);
        planService.findPlan(place.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(place.getPlanId());

        return updatePlaceService.update(place, planMembers, request, id);
    }

    @Transactional
    public void deletePlan(UUID id, DeletePlacesApplicationRequest request) {
        userService.findUserById(id);

        for (Long placeId : request.getPlaceId()) {
            PinPlace place = placeService.findPlace(placeId);
            planService.findPlan(place.getPlanId());
            List<PlanMember> planMembers = getPlanMembers(place.getPlanId());

            deletePlaceService.delete(place, planMembers, id);
        }
    }

    public PlaceResponse findPlan(UUID id, Long placeId) {
        userService.findUserById(id);

        PinPlace place = placeService.findPlace(placeId);
        boolean planPublic = planService.isPlanPublic(place.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(place.getPlanId());

        return findPlaceService.find(placeId, id, planMembers, place.getPlanId(), planPublic);
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
