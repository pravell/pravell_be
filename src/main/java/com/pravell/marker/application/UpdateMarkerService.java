package com.pravell.marker.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.marker.application.dto.request.UpdateMarkerApplicationRequest;
import com.pravell.marker.application.dto.response.MarkerResponse;
import com.pravell.marker.domain.model.Marker;
import com.pravell.marker.domain.model.PlanMember;
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
public class UpdateMarkerService {

    private final MarkerAuthorizationService markerAuthorizationService;

    @Transactional
    public MarkerResponse update(Marker marker, UUID userId, List<PlanMember> planMembers,
                                 UpdateMarkerApplicationRequest request) {
        validateUpdateMarker(marker, userId, planMembers);
        updateMarker(marker, request);
        return buildMarkerResponse(marker);
    }

    private void validateUpdateMarker(Marker marker, UUID userId, List<PlanMember> planMembers) {
        if (!markerAuthorizationService.isOwnerOrMember(userId, planMembers)) {
            log.info("{} 유저는 {} 플랜의 {} 마커를 수정 할 권한이 없습니다.", userId, marker.getPlanId(), marker.getId());
            throw new AccessDeniedException("마커를 수정 할 권한이 없습니다.");
        }
    }

    private void updateMarker(Marker marker, UpdateMarkerApplicationRequest request) {
        if (request.getColor() != null && !marker.getColor().equals(request.getColor())) {
            marker.updateColor(request.getColor());
        }
        if (request.getDescription() != null && !marker.getDescription().equals(request.getDescription())) {
            marker.updateDescription(request.getDescription());
        }
    }

    private MarkerResponse buildMarkerResponse(Marker marker) {
        return MarkerResponse.builder()
                .markerId(marker.getId())
                .planId(marker.getPlanId())
                .color(marker.getColor())
                .description(marker.getDescription())
                .build();
    }

}
