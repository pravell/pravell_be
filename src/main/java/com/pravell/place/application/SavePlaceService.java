package com.pravell.place.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.application.dto.request.SavePlaceApplicationRequest;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.place.domain.service.PlanAuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavePlaceService {

    private final PinPlaceRepository pinPlaceRepository;
    private final ObjectMapper objectMapper;
    private final PlanAuthorizationService planAuthorizationService;

    @Transactional
    public Long save(UUID id, SavePlaceApplicationRequest request, List<PlanMember> planMembers) {
        validatePlaceSavePermission(id, request, planMembers);
        checkDuplicatePlace(request);

        PinPlace pinPlace = createPinPlace(id, request);
        PinPlace saved = pinPlaceRepository.save(pinPlace);

        return saved.getId();
    }

    private void validatePlaceSavePermission(UUID id, SavePlaceApplicationRequest request, List<PlanMember> planMembers) {
        if (!planAuthorizationService.hasUpdatePermission(id, planMembers)){
            log.info("{} 유저는 {} 플랜에 장소를 저장 할 권한이 없습니다.", id, request.getPlanId());
            throw new AccessDeniedException("해당 플랜에 장소를 저장 할 권한이 없습니다.");
        }
    }

    private void checkDuplicatePlace(SavePlaceApplicationRequest request) {
        if (pinPlaceRepository.existsByPlanIdAndAddress(request.getPlanId(), request.getAddress())) {
            throw new DuplicateKeyException("해당 장소는 이미 저장되어 있습니다.");
        }
    }

    private PinPlace createPinPlace(UUID userId, SavePlaceApplicationRequest request) {
        try {
            return PinPlace.builder()
                    .placeId(request.getPlaceId())
                    .nickname(request.getNickname())
                    .title(request.getTitle())
                    .address(request.getAddress())
                    .roadAddress(request.getRoadAddress())
                    .hours(objectMapper.writeValueAsString(request.getHours()))
                    .mapx(request.getMapx())
                    .mapy(request.getMapy())
                    .pinColor(request.getPinColor())
                    .planId(request.getPlanId())
                    .savedUser(userId)
                    .description(request.getDescription())
                    .lastRefreshedAt(LocalDateTime.now())
                    .latitude(request.getLat())
                    .longitude(request.getLng())
                    .build();
        } catch (JsonProcessingException e) {
            log.warn("장소 저장 중 hours 필드 JSON 변환 중 오류", e);
            throw new RuntimeException("장소 저장 실패", e);
        }
    }

}
