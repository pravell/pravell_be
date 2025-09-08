package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.request.CreateRouteApplicationRequest;
import com.pravell.route.application.dto.response.CreateRouteResponse;
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
public class CreateRouteService {

    private final RouteRepository routeRepository;
    private final RouteAuthorizationService routeAuthorizationService;

    @Transactional
    public CreateRouteResponse create(UUID userId, CreateRouteApplicationRequest request,
                                      List<PlanMember> planMembers) {
        validateCreateRoute(userId, request, planMembers);
        Route saved = saveRoute(userId, request);
        return buildCreateRouteResponse(saved);
    }

    private void validateCreateRoute(UUID userId, CreateRouteApplicationRequest request, List<PlanMember> planMembers) {
        if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("{} 유저는 {} 플랜에 루트를 생성 할 권한이 없습니다.", userId, request.getPlanId());
            throw new AccessDeniedException("해당 루트를 생성 할 권한이 없습니다.");
        }
    }

    private Route saveRoute(UUID userId, CreateRouteApplicationRequest request) {
        Route saved = routeRepository.save(
                Route.create(request.getPlanId(), request.getName(), request.getDescription()));

        log.info("{} 유저가 {} 플랜에 {} 루트 생성.", userId, saved.getPlanId(), saved.getId());
        return saved;
    }

    private CreateRouteResponse buildCreateRouteResponse(Route saved) {
        return CreateRouteResponse.builder()
                .routeId(saved.getId())
                .planId(saved.getPlanId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

}
