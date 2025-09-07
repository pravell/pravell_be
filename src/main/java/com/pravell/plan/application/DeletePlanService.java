package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.service.PlanAuthorizationService;
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

    private final PlanAuthorizationService planAuthorizationService;

    @Transactional
    public void deletePlan(Plan plan, UUID userId, List<PlanUsers> planUsers) {
        log.info("{} 유저가 {} 플랜 삭제.", userId, plan.getId());
        validateOwnerPermission(userId, planUsers);
        plan.delete();
    }

    private void validateOwnerPermission(UUID id, List<PlanUsers> planUsers) {
        if (!planAuthorizationService.isOwner(id, planUsers)) {
            throw new AccessDeniedException("해당 리소스를 삭제 할 권한이 없습니다.");
        }
    }

}
