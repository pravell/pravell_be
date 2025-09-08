package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.request.UpdateRouteApplicationRequest;
import com.pravell.route.application.dto.response.RouteResponse;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.Route;
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
public class UpdateRouteService {

    private final RouteAuthorizationService routeAuthorizationService;

    @Transactional
    public RouteResponse update(UpdateRouteApplicationRequest request, Route route, UUID userId,
                                List<PlanMember> planMembers) {
        validateUpdateRoute(route, userId, planMembers);

        updateRoute(request, route, userId);

        return buildRouteResponse(route);
    }

    private void validateUpdateRoute(Route route, UUID userId, List<PlanMember> planMembers) {
        if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("{} 유저는 {} 루트를 수정 할 권한이 없습니다.", userId, route);
            throw new AccessDeniedException("해당 루트를 수정 할 권한이 없습니다.");
        }
    }

    private void updateRoute(UpdateRouteApplicationRequest request, Route route, UUID userId) {
        log.info("{} 유저가 {} 루트를 수정. before : {}, after : {}", userId, route.getId(), route.toString(),
                request.toString());

        if (request.getName() != null && !route.getName().equals(request.getName())) {
            route.updateName(request.getName());
        }
        if (request.getDescription() != null & !route.getDescription().equals(request.getDescription())) {
            route.updateDescription(request.getDescription());
        }
    }

    private RouteResponse buildRouteResponse(Route route) {
        return RouteResponse.builder()
                .routeId(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .createdAt(route.getCreatedAt())
                .build();
    }

}
