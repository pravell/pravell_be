package com.pravell.marker.application;

import com.pravell.common.exception.AccessDeniedException;
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
public class DeleteMarkerService {

    private final MarkerRepository markerRepository;
    private final MarkerAuthorizationService markerAuthorizationService;

    @Transactional
    public void delete(UUID id, Marker marker, List<PlanMember> planMembers) {
        validateDeletePermission(id, marker, planMembers);

        log.info("유저 {}가 {} 플랜의 {} 마커 삭제.", id, marker.getPlanId(), marker.getId());

        markerRepository.delete(marker);
    }

    private void validateDeletePermission(UUID id, Marker marker, List<PlanMember> planMembers) {
        if (!markerAuthorizationService.isOwnerOrMember(id, planMembers)) {
            log.info("유저 {}는 {} 플랜의 {} 마커를 삭제할 권한이 없습니다.", id, marker.getPlanId(), marker.getId());
            throw new AccessDeniedException("마커를 삭제할 권한이 없습니다.");
        }
    }

}
