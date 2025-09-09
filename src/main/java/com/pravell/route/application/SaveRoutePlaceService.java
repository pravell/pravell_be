package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.request.SaveRoutePlaceApplicationRequest;
import com.pravell.route.application.dto.response.RoutePlaceResponse;
import com.pravell.route.domain.model.Place;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.domain.repository.RoutePlaceRepository;
import com.pravell.route.domain.service.RouteAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaveRoutePlaceService {

    private final RouteAuthorizationService routeAuthorizationService;
    private final RoutePlaceRepository routePlaceRepository;

    @Transactional
    public RoutePlaceResponse save(SaveRoutePlaceApplicationRequest request, UUID routeId, UUID userId,
                                   List<PlanMember> planMembers, Place place) {
        validateSaveRoutePlace(routeId, userId, planMembers);
        RoutePlace saved = saveRoutePlace(request, routeId);
        log.info("{} 유저가 {} 루트에 {} 장소 저장", userId, routeId, saved.getId());
        return buildSaveRoutePlaceResponse(place, saved);
    }

    private void validateSaveRoutePlace(UUID routeId, UUID userId, List<PlanMember> planMembers) {
        if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("{} 유저는 {} 루트에 장소를 저장 할 권한이 없습니다.", userId, routeId);
            throw new AccessDeniedException("해당 장소를 저장 할 권한이 없습니다.");
        }
    }

    private RoutePlace saveRoutePlace(SaveRoutePlaceApplicationRequest request, UUID routeId) {
        Long maxSequence = routePlaceRepository.findMaxSequenceByRouteId(routeId).orElse(0L);
        return routePlaceRepository.save(
                RoutePlace.create(routeId, request.getPinPlaceId(), maxSequence + 1, request.getDescription(),
                        request.getNickname(), request.getDate()));
    }

    private RoutePlaceResponse buildSaveRoutePlaceResponse(Place place, RoutePlace saved) {
        return RoutePlaceResponse.builder()
                .routePlaceId(saved.getId())
                .pinPlaceId(saved.getPinPlaceId())
                .title(place.getTitle())
                .nickname(saved.getNickname())
                .description(saved.getDescription())
                .sequence(saved.getSequence())
                .date(saved.getDate())
                .address(place.getAddress())
                .roadAddress(place.getRoadAddress())
                .mapx(place.getMapx())
                .mapy(place.getMapy())
                .lat(place.getLat())
                .lng(place.getLng())
                .color(place.getColor())
                .isPinPlaceDeleted(null)
                .build();
    }

}
