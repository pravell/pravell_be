package com.pravell.marker.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.marker.application.dto.response.FindMarkersResponse;
import com.pravell.marker.domain.model.Marker;
import com.pravell.marker.domain.model.PlanMember;
import com.pravell.marker.domain.repository.MarkerRepository;
import com.pravell.marker.domain.service.MarkerAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindMarkerService {

    private final MarkerAuthorizationService markerAuthorizationService;
    private final MarkerRepository markerRepository;

    @Transactional(readOnly = true)
    public List<FindMarkersResponse> getMarkersOfPlan(UUID userId, boolean planPublic, List<PlanMember> planMembers,
                                                      UUID planId) {
        validateFindMarkers(userId, planPublic, planMembers, planId);

        List<Marker> markers = markerRepository.findAllByPlanId(planId);

        return buildFindMarkersResponse(markers);
    }

    private void validateFindMarkers(UUID userId, boolean planPublic, List<PlanMember> planMembers, UUID planId) {
        if (planPublic) {
            validatePublicPlanAccess(userId, planMembers, planId);
        } else {
            validatePrivatePlanAccess(userId, planMembers, planId);
        }
    }

    private void validatePublicPlanAccess(UUID userId, List<PlanMember> planMembers, UUID planId) {
        if (!markerAuthorizationService.hasPublicPlanPermission(userId, planMembers)) {
            log.info("유저 {}는 공개 플랜 {}의 마커를 조회할 수 없습니다.", userId, planId);
            throw new AccessDeniedException("해당 마커를 조회할 권한이 없습니다.");
        }
    }

    private void validatePrivatePlanAccess(UUID userId, List<PlanMember> planMembers, UUID planId) {
        if (!markerAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("유저 {}는 비공개 플랜 {}의 마커를 조회할 수 없습니다.", userId, planId);
            throw new AccessDeniedException("해당 마커를 조회할 권한이 없습니다.");
        }
    }

    private List<FindMarkersResponse> buildFindMarkersResponse(List<Marker> markers) {
        return markers.stream()
                .map(m -> FindMarkersResponse.builder()
                        .color(m.getColor())
                        .description(m.getDescription())
                        .build())
                .toList();
    }

}
