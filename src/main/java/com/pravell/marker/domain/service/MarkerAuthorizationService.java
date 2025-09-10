package com.pravell.marker.domain.service;

import com.pravell.marker.domain.model.PlanMember;
import com.pravell.marker.domain.model.PlanMemberStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MarkerAuthorizationService {

    public boolean isOwnerOrMember(UUID userId, List<PlanMember> planMembers) {
        return planMembers.stream()
                .anyMatch(pm ->
                        pm.getMemberId().equals(userId) &&
                                (pm.getPlanMemberStatus() == PlanMemberStatus.OWNER ||
                                        pm.getPlanMemberStatus() == PlanMemberStatus.MEMBER));
    }

    public boolean hasPublicPlanPermission(UUID userId, List<PlanMember> planMembers){
        return planMembers.stream()
                .noneMatch(pm->
                        pm.getMemberId().equals(userId) && pm.getPlanMemberStatus().equals(PlanMemberStatus.BLOCKED));
    }

}
