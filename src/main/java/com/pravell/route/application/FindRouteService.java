package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.response.FindRoutesResponse;
import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.repository.RouteRepository;
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
public class FindRouteService {

    private final RouteRepository routeRepository;
    private final RouteAuthorizationService routeAuthorizationService;

    @Transactional(readOnly = true)
    public List<FindRoutesResponse> findAll(UUID userId, UUID planId, List<PlanMember> planMembers, boolean isPublic) {
        validateRouteFind(userId, planId, planMembers, isPublic);

        List<Route> routes = routeRepository.findAllByPlanId(planId);

        return buildFindRoutesResponse(routes);
    }

    private void validateRouteFind(UUID userId, UUID planId, List<PlanMember> planMembers, boolean isPublic) {
        if (isPublic) {
            if (!routeAuthorizationService.hasPublicRoutePermission(userId, planMembers)) {
                denyAccess(userId, planId);
            }
        } else {
            if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)) {
                denyAccess(userId, planId);
            }
        }
    }

    private void denyAccess(UUID userId, UUID planId) {
        log.info("{} 유저는 {} 플랜의 루트들을 볼 수 없습니다.", userId, planId);
        throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
    }

    private List<FindRoutesResponse> buildFindRoutesResponse(List<Route> routes) {
        return routes.stream().map(r -> {
            return FindRoutesResponse.builder()
                    .routeId(r.getId())
                    .name(r.getName())
                    .description(r.getDescription())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).toList();
    }

}
