package com.pravell.place.domain.service;

import com.pravell.place.domain.model.PlanMember;
import com.pravell.place.domain.model.PlanMemberStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlaceAuthorizationService {

    public boolean hasUpdatePermission(UUID userId, List<PlanMember> planMembers) {
        return planMembers.stream()
                .anyMatch(pm ->
                        pm.getMemberId().equals(userId) &&
                                (pm.getPlanMemberStatus() == PlanMemberStatus.OWNER ||
                                        pm.getPlanMemberStatus() == PlanMemberStatus.MEMBER));
    }

}
