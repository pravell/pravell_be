package com.pravell.route.application;

import com.pravell.place.application.PlaceService;
import com.pravell.place.application.dto.PlaceDTO;
import com.pravell.plan.application.PlanService;
import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.route.application.dto.request.DeleteRoutePlacesApplicationRequest;
import com.pravell.route.application.dto.request.SaveRoutePlaceApplicationRequest;
import com.pravell.route.application.dto.request.UpdatePlaceApplicationRequest;
import com.pravell.route.application.dto.response.RoutePlaceResponse;
import com.pravell.route.domain.exception.RoutePlaceNotFoundException;
import com.pravell.route.domain.model.Place;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.PlanMemberStatus;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.model.RoutePlace;
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
    private final FindRoutePlaceService findRoutePlaceService;
    private final UpdateRoutePlaceService updateRoutePlaceService;
    private final RoutePlaceService routePlaceService;
    private final DeleteRoutePlaceService deleteRoutePlaceService;

    public RoutePlaceResponse savePlace(UUID userId, UUID routeId, SaveRoutePlaceApplicationRequest request) {
        userService.findUserById(userId);

        Route route = routeService.findById(routeId);
        planService.findPlan(route.getPlanId());
        Place place = getPlace(request.getPinPlaceId(), route);

        List<PlanMember> planMembers = getPlanMembers(route.getPlanId());

        return saveRoutePlaceService.save(request, routeId, userId, planMembers, place);
    }

    public List<RoutePlaceResponse> findPlaces(UUID userId, UUID routeId) {
        userService.findUserById(userId);

        Route route = routeService.findById(routeId);
        boolean planPublic = planService.isPlanPublic(route.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(route.getPlanId());

        List<RoutePlace> routePlaces = findRoutePlaceService.findAllPlaces(routeId);
        List<Place> places = getPlaces(routePlaces);

        return findRoutePlaceService.findAll(userId, planPublic, planMembers, places, routePlaces, routeId);
    }

    private List<Place> getPlaces(List<RoutePlace> routePlaces) {
        List<Long> placeIds = routePlaces.stream()
                .map(RoutePlace::getPinPlaceId)
                .toList();

        List<PlaceDTO> placeDTOs = placeService.findAllByPlaceIds(placeIds);

        return placeDTOs.stream()
                .map(this::convertToPlace)
                .toList();
    }

    private Place convertToPlace(PlaceDTO dto) {
        return Place.builder()
                .pinPlaceId(dto.getPinPlaceId())
                .title(dto.getTitle())
                .address(dto.getAddress())
                .roadAddress(dto.getRoadAddress())
                .mapx(dto.getMapx())
                .mapy(dto.getMapy())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .color(dto.getColor())
                .build();
    }

    public RoutePlaceResponse updatePlace(UUID routeId, Long routePlaceId, UUID userId,
                                          UpdatePlaceApplicationRequest request) {
        userService.findUserById(userId);

        Route route = routeService.findById(routeId);
        planService.findPlan(route.getPlanId());
        List<PlanMember> planMembers = getPlanMembers(route.getPlanId());
        RoutePlace routePlace = routePlaceService.findRoutePlace(routePlaceId);

        Place place = getPlace(request, route, routePlace);

        return updateRoutePlaceService.update(routePlace, request, userId, planMembers, route.getPlanId(), place);
    }

    private Place getPlace(UpdatePlaceApplicationRequest request, Route route, RoutePlace routePlace) {
        return request.getPinPlaceId() == null ?
                getPlace(routePlace.getPinPlaceId(), route) : getPlace(request.getPinPlaceId(), route);
    }

    public void deletePlaces(UUID routeId, DeleteRoutePlacesApplicationRequest request, UUID userId) {
        userService.findUserById(userId);

        List<PlanMember> planMembers = getPlanMember(routeId);

        validateRoutePlaceInRoute(routeId, request);
        deleteRoutePlaceService.deleteAll(request, userId, planMembers, routeId);
    }

    private void validateRoutePlaceInRoute(UUID routeId, DeleteRoutePlacesApplicationRequest request) {
        boolean exists = routePlaceService.existsRoutePlaceInRoute(routeId, request.getDeleteRoutePlaceId());
        if (!exists) {
            throw new RoutePlaceNotFoundException("루트에서 해당 장소를 찾을 수 없습니다.");
        }
    }

    private List<PlanMember> getPlanMember(UUID routeId) {
        Route route = routeService.findById(routeId);
        planService.findPlan(route.getPlanId());
        return getPlanMembers(route.getPlanId());
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

    private Place getPlace(Long pinPlaceId, Route route) {
        PlaceDTO placeDto = placeService.findPlaceByPlaceIdAndPlanId(pinPlaceId, route.getPlanId());

        return Place.builder()
                .pinPlaceId(placeDto.getPinPlaceId())
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
