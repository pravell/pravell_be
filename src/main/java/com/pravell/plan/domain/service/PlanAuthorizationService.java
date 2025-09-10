package com.pravell.plan.domain.service;

import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlanAuthorizationService {

    public boolean isOwnerOrMember(UUID userId, List<PlanUsers> planUsers) {
        return planUsers.stream()
                .anyMatch(pm ->
                        pm.getUserId().equals(userId) &&
                                (pm.getPlanUserStatus() == PlanUserStatus.OWNER ||
                                        pm.getPlanUserStatus() == PlanUserStatus.MEMBER));
    }

    public boolean hasPublicPlanPermission(UUID userId, List<PlanUsers> planUsers) {
        return planUsers.stream()
                .noneMatch(pm ->
                        pm.getUserId().equals(userId) && pm.getPlanUserStatus().equals(PlanUserStatus.BLOCKED));
    }

    public boolean isOwner(UUID userId, List<PlanUsers> planUsers) {
        return planUsers.stream()
                .anyMatch(pu -> pu.getUserId().equals(userId) && pu.getPlanUserStatus().equals(PlanUserStatus.OWNER));
    }

}
