package com.pravell.route.domain.service;

import com.pravell.route.domain.model.PlanMember;
import com.pravell.route.domain.model.PlanMemberStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RouteAuthorizationService {

    public boolean isOwnerOrMember(UUID userId, List<PlanMember> planMembers) {
        return planMembers.stream()
                .anyMatch(pm ->
                        pm.getMemberId().equals(userId) &&
                                (pm.getPlanMemberStatus() == PlanMemberStatus.OWNER ||
                                        pm.getPlanMemberStatus() == PlanMemberStatus.MEMBER));
    }

    public boolean hasPublicRoutePermission(UUID userId, List<PlanMember> planMembers) {
        return planMembers.stream()
                .noneMatch(pm ->
                        pm.getMemberId().equals(userId) && pm.getPlanMemberStatus().equals(PlanMemberStatus.BLOCKED));
    }

    public boolean isOwner(UUID userId, List<PlanMember> planMembers) {
        return planMembers.stream()
                .anyMatch(pu -> pu.getMemberId().equals(userId) && pu.getPlanMemberStatus()
                        .equals(PlanMemberStatus.OWNER));
    }

}
