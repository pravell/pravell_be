package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.application.dto.request.UpdatePlanApplicationRequest;
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
public class UpdatePlanService {

    private final PlanAuthorizationService planAuthorizationService;

    @Transactional
    public void update(Plan plan, UUID userId, List<PlanUsers> planUsers,
                       UpdatePlanApplicationRequest request) {
        log.info("{}유저가 {} plan 업데이트. before : {}, after : {}",
                userId, plan.getId(), plan.toString(), request.toString());

        validateUpdatePermission(userId, planUsers);

        if (request.getIsPublic() != null) {
            plan.updatePublic(request.getIsPublic());
        }
        if (request.getName() != null) {
            plan.updateName(request.getName());
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            plan.updateDate(request.getStartDate(), request.getEndDate());
        }
        if (request.getStartDate() != null) {
            plan.updateStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            plan.updateEndDate(request.getEndDate());
        }
    }

    private void validateUpdatePermission(UUID userId, List<PlanUsers> planUsers) {
        if (!planAuthorizationService.isOwner(userId, planUsers)){
            throw new AccessDeniedException("해당 리소스를 수정 할 권한이 없습니다.");
        }
    }

}
