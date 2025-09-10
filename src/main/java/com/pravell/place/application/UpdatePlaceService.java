package com.pravell.place.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.application.dto.request.UpdatePlaceApplicationRequest;
import com.pravell.place.application.dto.response.PlaceResponse;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.service.PlaceAuthorizationService;
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
    private final PlaceAuthorizationService placeAuthorizationService;

    @Transactional
    public PlaceResponse update(PinPlace place, List<PlanMember> planMembers, UpdatePlaceApplicationRequest request,
                                UUID id) {
        validateUpdatePermission(place, planMembers, id);
        log.info("{} 유저가 {} 장소 수정. before : {} after {}", id, place.getId(), place.toString(), request.toString());

        updatePlaceFields(place, request);
        List<String> hoursList = parseHours(place);

        return buildPlaceResponse(place, hoursList);
    }

    private void validateUpdatePermission(PinPlace place, List<PlanMember> planMembers, UUID id) {
        if (!placeAuthorizationService.hasUpdatePermission(id, planMembers)){
            log.info("{} 유저는 {} 플랜을 수정 할 권한이 없습니다.", id, place.getPlanId());
            throw new AccessDeniedException("해당 장소를 수정 할 권한이 없습니다.");
        }
    }

    private static void updatePlaceFields(PinPlace place, UpdatePlaceApplicationRequest request) {
        Optional.ofNullable(request.getNickname())
                .filter(s -> !s.trim().isEmpty())
                .ifPresent(place::updateNickname);

        Optional.ofNullable(request.getPinColor())
                .filter(s -> !s.trim().isEmpty())
                .ifPresent(place::updatePinColor);

        Optional.ofNullable(request.getDescription())
                .filter(s -> !s.trim().isEmpty())
                .ifPresent(place::updateDescription);
    }

    private List<String> parseHours(PinPlace place) {
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
        return hoursList;
    }

    private static PlaceResponse buildPlaceResponse(PinPlace place, List<String> hoursList) {
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

}
