package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeletePlanService {

    @Transactional
    public void deletePlan(Plan plan, UUID userId, List<PlanUsers> planUsers) {
        validateOwnerPermission(userId, planUsers);
        plan.delete();
    }

    private void validateOwnerPermission(UUID id, List<PlanUsers> planUsers) {
        boolean isOwner = planUsers.stream()
                .anyMatch(pu -> pu.getUserId().equals(id) && pu.getPlanUserStatus().equals(PlanUserStatus.OWNER));

        if (!isOwner) {
            throw new AccessDeniedException("해당 리소스를 삭제 할 권한이 없습니다.");
        }
    }

}
