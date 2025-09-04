package com.pravell.place.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.application.dto.request.SavePlaceApplicationRequest;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.model.PlanMemberStatus;
import com.pravell.place.domain.repository.PinPlaceRepository;
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

    @Transactional
    public Long save(UUID id, SavePlaceApplicationRequest request, List<PlanMember> planMembers) {
        validatePlaceSave(id, planMembers, request.getPlanId());

        if (pinPlaceRepository.existsByPlanIdAndAddress(request.getPlanId(), request.getAddress())) {
            throw new DuplicateKeyException("해당 장소는 이미 저장되어 있습니다.");
        }

        Long pinPlaceId = null;

        try {
            PinPlace save = pinPlaceRepository.save(PinPlace.builder()
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
                    .savedUser(id)
                    .description(request.getDescription())
                    .lastRefreshedAt(LocalDateTime.now())
                    .latitude(request.getLat())
                    .longitude(request.getLng())
                    .build());

            pinPlaceId = save.getId();
        } catch (JsonProcessingException e) {
            log.warn("장소 저장 중 hours 필드 JSON 변환 중 오류", e);
            throw new RuntimeException("장소 저장 실패", e);
        }

        return pinPlaceId;
    }

    private void validatePlaceSave(UUID id, List<PlanMember> planMembers, UUID planId) {
        boolean isMember = planMembers.stream().anyMatch(pm -> pm.getMemberId().equals(id) &&
                !pm.getPlanMemberStatus().equals(PlanMemberStatus.BLOCKED));

        if (!isMember) {
            log.info("{} 유저는 {} 플랜에 장소를 저장 할 권한이 없습니다.", id, planId);
            throw new AccessDeniedException("해당 플랜에 장소를 저장 할 권한이 없습니다.");
        }
    }

}
