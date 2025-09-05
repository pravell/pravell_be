package com.pravell.marker.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.marker.application.dto.request.CreateMarkerApplicationRequest;
import com.pravell.marker.application.dto.response.MarkerResponse;
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
@Slf4j
@RequiredArgsConstructor
public class CreateMarkerService {

    private final MarkerRepository markerRepository;
    private final MarkerAuthorizationService markerAuthorizationService;

    @Transactional
    public MarkerResponse create(UUID id, List<PlanMember> planMembers, CreateMarkerApplicationRequest request) {
        validateCreateMarker(id, planMembers, request.getPlanId());

        log.info("유저 {}가 {} 플랜에 마커를 생성했습니다.", id, request.getPlanId());

        Marker saved = markerRepository.save(
                Marker.createMarker(request.getDescription(), request.getColor(), request.getPlanId()));

        return buildCreateMarkerResponse(saved);
    }

    private void validateCreateMarker(UUID id, List<PlanMember> planMembers, UUID planId) {
        if (!markerAuthorizationService.isOwnerOrMember(id, planMembers)) {
            log.info("유저 {}는 {} 플랜에 마커를 생성 할 권한이 없습니다.", id, planId);
            throw new AccessDeniedException("해당 마커를 생성 할 권한이 없습니다.");
        }
    }

    private MarkerResponse buildCreateMarkerResponse(Marker saved) {
        return MarkerResponse.builder()
                .markerId(saved.getId())
                .planId(saved.getPlanId())
                .color(saved.getColor())
                .description(saved.getDescription())
                .build();
    }

}
