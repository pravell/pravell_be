package com.pravell.route.application;

import com.pravell.place.application.PlaceService;
import com.pravell.place.application.dto.PlaceDTO;
import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.route.application.dto.request.SaveRoutePlaceApplicationRequest;
import com.pravell.route.application.dto.response.SaveRoutePlaceResponse;
import com.pravell.route.domain.model.Place;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.PlanMemberStatus;
import com.pravell.route.domain.model.Route;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutePlaceFacade {

    private final UserService userService;
    private final PlanService planService;
    private final RouteService routeService;
    private final PlaceService placeService;
    private final SaveRoutePlaceService saveRoutePlaceService;

    public SaveRoutePlaceResponse savePlace(UUID userId, UUID routeId, SaveRoutePlaceApplicationRequest request) {
        userService.findUserById(userId);

        Route route = routeService.findById(routeId);
        planService.findPlan(route.getPlanId());
        Place place = getPlace(request, route);

        List<PlanMember> planMembers = getPlanMembers(route.getPlanId());

        return saveRoutePlaceService.save(request, routeId, userId, planMembers, place);
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

    private Place getPlace(SaveRoutePlaceApplicationRequest request, Route route) {
        PlaceDTO placeDto = placeService.findPlaceByPlaceIdAndPlanId(request.getPinPlaceId(), route.getPlanId());

        return Place.builder()
                .title(placeDto.getTitle())
                .address(placeDto.getAddress())
                .roadAddress(placeDto.getRoadAddress())
                .mapx(placeDto.getMapx())
                .mapy(placeDto.getMapy())
                .lat(placeDto.getLat())
                .lng(placeDto.getLng())
                .color(placeDto.getColor())
                .build();
    }

}
