package com.pravell.route.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.route.application.dto.request.DeleteRoutePlacesApplicationRequest;
import com.pravell.route.domain.model.PlanMember;
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
public class DeleteRoutePlaceService {

    private final RouteAuthorizationService routeAuthorizationService;
    private final RoutePlaceRepository routePlaceRepository;

    @Transactional
    public void deleteAll(DeleteRoutePlacesApplicationRequest request, UUID userId, List<PlanMember> planMembers,
                          UUID routeId) {
        validateDeleteRoutePlaces(userId, planMembers, routeId);
        routePlaceRepository.deleteAllById(request.getDeleteRoutePlaceId());
    }

    private void validateDeleteRoutePlaces(UUID userId, List<PlanMember> planMembers, UUID routeId) {
        if (!routeAuthorizationService.isOwnerOrMember(userId, planMembers)){
            log.info("{} 유저는 {} 루트에 속한 장소들을 삭제 할 권한이 없습니다.", userId, routeId);
            throw new AccessDeniedException("해당 장소를 삭제 할 권한이 없습니다.");
        }
    }

}
