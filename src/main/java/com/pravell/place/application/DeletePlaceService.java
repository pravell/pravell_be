package com.pravell.place.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.place.domain.service.PlaceAuthorizationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeletePlaceService {

    private final PlaceAuthorizationService placeAuthorizationService;
    private final PinPlaceRepository pinPlaceRepository;

    @Transactional
    public void delete(PinPlace place, List<PlanMember> planMembers, UUID id) {
        validatePlaceDeletionPermission(planMembers, id, place.getPlanId(), place.getId());
        pinPlaceRepository.delete(place);
    }

    private void validatePlaceDeletionPermission(List<PlanMember> planMembers, UUID id, UUID planId, Long placeId) {
        if (!placeAuthorizationService.hasUpdatePermission(id, planMembers)) {
            log.info("{} 유저는 {} 플랜의 {} 장소를 삭제 할 권한이 없습니다.", id, planId, placeId);
            throw new AccessDeniedException("해당 장소를 삭제 할 권한이 없습니다.");
        }
    }

}
