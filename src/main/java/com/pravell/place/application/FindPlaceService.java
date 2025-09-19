package com.pravell.place.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.application.dto.response.FindPlanPlacesResponse;
import com.pravell.place.application.dto.response.PlaceResponse;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.model.PlanMemberStatus;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.plan.domain.exception.PlanNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindPlaceService {

    private final PinPlaceRepository pinPlaceRepository;
    private final ObjectMapper objectMapper;

    @Value("${naver.map.url}")
    private String mapUrl;

    @Transactional(readOnly = true)
    public List<FindPlanPlacesResponse> findAll(UUID userId, UUID planId, List<PlanMember> planMembers,
                                                boolean isPlanPublic) {
        validateAccessToPlan(userId, planMembers, planId, isPlanPublic);
        List<PinPlace> pinPlaces = pinPlaceRepository.findAllByPlanId(planId);
        return buildFindPlanPlacesResponses(pinPlaces);
    }

    @Transactional(readOnly = true)
    public PlaceResponse find(Long placeId, UUID id, List<PlanMember> planMembers, UUID planId, boolean isPlanPublic) {
        validateAccessToPlan(id, planMembers, planId, isPlanPublic);

        PinPlace place = getPlan(placeId);
        List<String> hours = parseHours(place);

        return buildPlaceResponse(place, hours);
    }

    private List<FindPlanPlacesResponse> buildFindPlanPlacesResponses(List<PinPlace> pinPlaces) {
        return pinPlaces.stream().map(pp -> {
            List<String> hours = parseHours(pp);
            return FindPlanPlacesResponse.builder()
                    .id(pp.getId())
                    .nickname(pp.getNickname())
                    .title(pp.getTitle())
                    .mapx(pp.getMapx())
                    .mapy(pp.getMapy())
                    .lat(pp.getLatitude())
                    .lng(pp.getLongitude())
                    .pinColor(pp.getPinColor())
                    .address(pp.getAddress())
                    .roadAddress(pp.getRoadAddress())
                    .hours(hours)
                    .mapUrl(mapUrl + pp.getTitle().replaceAll("\\s+", ""))
                    .build();
        }).toList();
    }

    private void validateAccessToPlan(UUID userId, List<PlanMember> planMembers, UUID planId, boolean isPlanPublic) {
        boolean isBlocked = planMembers.stream()
                .anyMatch(pm -> pm.getMemberId().equals(userId) &&
                        pm.getPlanMemberStatus() == PlanMemberStatus.BLOCKED);

        if (isBlocked) {
            log.info("{} 유저는 {} 플랜에서 블락되어 접근 할 수 없습니다.", userId, planId);
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }

        if (!isPlanPublic) {
            boolean isMember = planMembers.stream()
                    .anyMatch(pm -> pm.getMemberId().equals(userId));

            if (!isMember) {
                log.info("{} 플랜은 비공개 플랜으로, {} 유저가 접근할 수 없습니다.", planId, userId);
                throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
            }
        }
    }

    private PinPlace getPlan(Long placeId) {
        return pinPlaceRepository.findById(placeId)
                .orElseThrow(() -> new PlanNotFoundException("장소를 찾을 수 없습니다."));
    }

    private List<String> parseHours(PinPlace place) {
        List<String> hoursList = new ArrayList<>();
        try {
            String json = place.getHours();
            if (StringUtils.hasText(json) && !json.equals("정보 없음")) {
                hoursList = objectMapper.readValue(json, new TypeReference<>() {
                });
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
