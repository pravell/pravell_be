package com.pravell.place.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.place.application.dto.response.FindPlanPlacesResponse;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.model.PlanMemberStatus;
import com.pravell.place.domain.repository.PinPlaceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FindPlaceService {

    private final PinPlaceRepository pinPlaceRepository;

    @Transactional(readOnly = true)
    public List<FindPlanPlacesResponse> findAll(UUID userId, UUID planId, List<PlanMember> planMembers,
                                                boolean isPlanPublic) {
        validateAccessToPlan(userId, planMembers, planId, isPlanPublic);

        List<PinPlace> pinPlaces = pinPlaceRepository.findAllByPlanId(planId);

        return pinPlaces.stream().map(pp -> {
            return FindPlanPlacesResponse.builder()
                    .id(pp.getId())
                    .nickname(pp.getNickname())
                    .title(pp.getTitle())
                    .mapx(pp.getMapx())
                    .mapy(pp.getMapy())
                    .lat(pp.getLatitude())
                    .lng(pp.getLongitude())
                    .pinColor(pp.getPinColor())
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

}
