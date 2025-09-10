package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
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
@RequiredArgsConstructor
@Slf4j
public class DeleteRouteService {

    private final RouteAuthorizationService routeAuthorizationService;

    @Transactional
    public void delete(Route route, UUID userId, List<PlanMember> planMembers) {
        validateDeleteRoute(route, userId, planMembers);
        route.delete();
    }

    private void validateDeleteRoute(Route route, UUID userId, List<PlanMember> planMembers) {
        if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)){
            log.info("{} 유저는 {} 루트를 삭제 할 권한이 없습니다.", userId, route.getId());
            throw new AccessDeniedException("해당 루트를 삭제 할 권한이 없습니다.");
        }
    }

}
