package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.response.RoutePlaceResponse;
import com.pravell.route.domain.model.Place;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.domain.repository.RoutePlaceRepository;
import com.pravell.route.domain.service.RouteAuthorizationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindRoutePlaceService {

    private final RoutePlaceRepository routePlaceRepository;
    private final RouteAuthorizationService routeAuthorizationService;

    @Transactional(readOnly = true)
    public List<RoutePlace> findAllPlaces(UUID routeId) {
        return routePlaceRepository.findAllByRouteId(routeId);
    }

    public List<RoutePlaceResponse> findAll(UUID userId, boolean planPublic, List<PlanMember> planMembers,
                                            List<Place> places,
                                            List<RoutePlace> routePlaces, UUID routeId) {
        validateRoutePlaceFind(userId, planPublic, planMembers, routeId);

        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getPinPlaceId, Function.identity()));

        return buildRoutePlaceResponse(routePlaces, placeMap);
    }

    private void validateRoutePlaceFind(UUID userId, boolean planPublic, List<PlanMember> planMembers, UUID routeId) {
        if (planPublic) {
            if (!routeAuthorizationService.hasPublicRoutePermission(userId, planMembers)) {
                log.info("{} 유저는 {} 루트의 장소를 조회 할 권한이 없습니다.", userId, routeId);
                throw new AccessDeniedException("해당 장소를 조회 할 권한이 없습니다.");
            }
        } else {
            if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)) {
                log.info("{} 유저는 {} 루트의 장소를 조회 할 권한이 없습니다.", userId, routeId);
                throw new AccessDeniedException("해당 장소를 조회 할 권한이 없습니다.");
            }
        }
    }

    private List<RoutePlaceResponse> buildRoutePlaceResponse(List<RoutePlace> routePlaces,
                                                             Map<Long, Place> placeMap) {
        return routePlaces.stream().map(r -> {
            Place place = placeMap.get(r.getPinPlaceId());
            if (place == null) {
                return toEmptyPlaceResponse(r);
            }

            return toPlaceResponse(r, place);
        }).toList();
    }

    private static RoutePlaceResponse toPlaceResponse(RoutePlace r, Place place) {
        return RoutePlaceResponse.builder()
                .routePlaceId(r.getId())
                .pinPlaceId(place.getPinPlaceId())
                .title(place.getTitle())
                .nickname(r.getNickname())
                .description(r.getDescription())
                .sequence(r.getSequence())
                .date(r.getDate())
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

    private static RoutePlaceResponse toEmptyPlaceResponse(RoutePlace r) {
        return RoutePlaceResponse.builder()
                .routePlaceId(r.getId())
                .pinPlaceId(r.getPinPlaceId())
                .nickname(r.getNickname())
                .description(r.getDescription())
                .sequence(r.getSequence())
                .date(r.getDate())
                .isPinPlaceDeleted(true)
                .build();
    }

}
