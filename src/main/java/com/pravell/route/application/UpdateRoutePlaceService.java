package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.request.UpdatePlaceApplicationRequest;
import com.pravell.route.application.dto.response.RoutePlaceResponse;
import com.pravell.route.domain.model.Place;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.domain.service.RouteAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateRoutePlaceService {

    private final RouteAuthorizationService routeAuthorizationService;

    @Transactional
    public RoutePlaceResponse update(RoutePlace routePlace, UpdatePlaceApplicationRequest request,
                                     UUID userId, List<PlanMember> planMembers, UUID planId, Place place) {
        validateUpdateRoutePlace(routePlace.getId(), userId, planMembers, planId);

        log.info("{} 유저가 {} 플랜의 {} 장소 업데이트. before : {}, after : {}",
                userId, planId, routePlace.getRouteId(), routePlace.toString(), request.toString());

        updateRoutePlace(request, routePlace);
        return buildRoutePlaceResponse(routePlace, place);
    }

    private void validateUpdateRoutePlace(Long routePlaceId, UUID userId, List<PlanMember> planMembers, UUID planId) {
        if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("{} 유저는 {} 플랜의 {} 장소를 수정 할 권한이 없습니다.", userId, planId, routePlaceId);
            throw new AccessDeniedException("해당 장소를 수정 할 권한이 없습니다.");
        }
    }

    private void updateRoutePlace(UpdatePlaceApplicationRequest request, RoutePlace routePlace) {
        if (request.getPinPlaceId() != null && !request.getPinPlaceId().equals(routePlace.getPinPlaceId())) {
            routePlace.updatePinPlaceId(request.getPinPlaceId());
        }
        if (request.getDescription() != null && !request.getDescription().equals(routePlace.getDescription())) {
            routePlace.updateDescription(request.getDescription());
        }
        if (request.getNickname() != null && !request.getNickname().equals(routePlace.getNickname())) {
            routePlace.updateNickname(request.getNickname());
        }
        if (request.getSequence() != null && !request.getSequence().equals(routePlace.getSequence())) {
            routePlace.updateSequence(request.getSequence());
        }
        if (request.getDescription() != null && !request.getDate().equals(routePlace.getDate())) {
            routePlace.updateDate(request.getDate());
        }
    }

    private RoutePlaceResponse buildRoutePlaceResponse(RoutePlace routePlace, Place place) {
        return RoutePlaceResponse.builder()
                .routePlaceId(routePlace.getId())
                .pinPlaceId(place.getPinPlaceId())
                .title(place.getTitle())
                .nickname(routePlace.getNickname())
                .description(routePlace.getDescription())
                .sequence(routePlace.getSequence())
                .date(routePlace.getDate())
                .address(place.getAddress())
                .roadAddress(place.getRoadAddress())
                .mapx(place.getMapx())
                .mapy(place.getMapy())
                .lat(place.getLat())
                .lng(place.getLng())
                .color(place.getColor())
                .isPinPlaceDeleted(false)
                .build();
    }

}
