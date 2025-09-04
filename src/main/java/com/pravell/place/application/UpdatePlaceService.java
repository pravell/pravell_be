package com.pravell.place.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.application.dto.request.UpdatePlaceApplicationRequest;
import com.pravell.place.application.dto.response.PlaceResponse;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.model.PlanMemberStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdatePlaceService {

    private final ObjectMapper objectMapper;

    @Transactional
    public PlaceResponse update(PinPlace place, List<PlanMember> planMembers, UpdatePlaceApplicationRequest request,
                                UUID id) {
        validatePlaceUpdate(id, planMembers);

        log.info("{} 유저가 {} 장소 수정. before : {} after {}", id, place.getId(), place.toString(), request.toString());

        Optional.ofNullable(request.getNickname())
                .filter(s -> !s.trim().isEmpty())
                .ifPresent(place::updateNickname);

        Optional.ofNullable(request.getPinColor())
                .filter(s -> !s.trim().isEmpty())
                .ifPresent(place::updatePinColor);

        Optional.ofNullable(request.getDescription())
                .filter(s -> !s.trim().isEmpty())
                .ifPresent(place::updateDescription);

        List<String> hoursList = new ArrayList<>();
        try {
            String json = place.getHours();
            if (StringUtils.hasText(json) && !json.equals("정보 없음")) {
                hoursList = objectMapper.readValue(json, new TypeReference<>() {});
            } else {
                hoursList.add("정보 없음");
            }
        } catch (Exception e) {
            log.warn("{} place hoursList 파싱 실패. e : {}", place.getId(), e.getMessage());
            throw new RuntimeException("파싱 실패.");
        }

        return PlaceResponse.builder()
                .id(place.getId())
                .nickname(place.getNickname())
                .title(place.getTitle())
                .address(place.getAddress())
                .roadAddress(place.getRoadAddress())
                .hours(hoursList)
                .mapx(place.getMapx())
                .mapy(place.getMapy())
                .lat(place.getLatitude())
                .lng(place.getLongitude())
                .pinColor(place.getPinColor())
                .planId(place.getPlanId())
                .description(place.getDescription())
                .build();
    }

    private void validatePlaceUpdate(UUID userId, List<PlanMember> planMembers) {
        boolean isMember = planMembers.stream()
                .anyMatch(pm ->
                        pm.getMemberId().equals(userId) &&
                                (pm.getPlanMemberStatus() == PlanMemberStatus.OWNER ||
                                        pm.getPlanMemberStatus() == PlanMemberStatus.MEMBER));

        if (!isMember) {
            throw new AccessDeniedException("해당 장소를 수정 할 권한이 없습니다.");
        }
    }
}
