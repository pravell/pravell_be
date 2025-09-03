package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
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
public class UpdatePlanService {

    @Transactional
    public void update(Plan plan, UUID userId, List<PlanUsers> planUsers,
                       CreatePlanApplicationRequest applicationRequest) {
        log.info("{}유저가 {} plan 업데이트. before : {}, after : {}",
                userId, plan.getId(), plan.toString(), applicationRequest.toString());

        validateUpdatePermission(userId, planUsers);

        if (applicationRequest.getIsPublic() != null) {
            plan.updatePublic(applicationRequest.getIsPublic());
        }
        if (applicationRequest.getName() != null) {
            plan.updateName(applicationRequest.getName());
        }
    }

    private static void validateUpdatePermission(UUID userId, List<PlanUsers> planUsers) {
        boolean isOwner = planUsers.stream()
                .anyMatch(pu -> pu.getUserId().equals(userId) && pu.getPlanUserStatus().equals(PlanUserStatus.OWNER));

        if (!isOwner) {
            throw new AccessDeniedException("해당 리소스를 수정 할 권한이 없습니다.");
        }
    }

}
