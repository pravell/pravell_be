package com.pravell.plan.application;

import com.pravell.plan.application.dto.request.WithdrawFromPlansApplicationRequest;
import com.pravell.plan.domain.exception.PlanNotFoundException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WithdrawPlanService {

    private final PlanUsersRepository planUsersRepository;
    private final PlanRepository planRepository;

    @Transactional
    public void withdrawFromPlans(UUID userId, WithdrawFromPlansApplicationRequest request) {
        for (UUID planId : request.getPlanIds()) {
            findPlan(planId);
            withdraw(userId, planId);
        }
    }

    private void withdraw(UUID userId, UUID planId) {
        PlanUsers planUsers = planUsersRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new PlanNotFoundException("해당 플랜에 유저가 존재하지 않습니다."));

        switch (planUsers.getPlanUserStatus()) {
            case MEMBER -> planUsers.updateToWithdrawn();
            case OWNER -> throw new IllegalArgumentException("플랜을 소유한 유저는 탈퇴할 수 없습니다.");
            default -> throw new PlanNotFoundException("해당 플랜에 유저가 존재하지 않습니다.");
        }
    }

    private void findPlan(UUID planId) {
        Optional<Plan> plan = planRepository.findById(planId);
        if (plan.isEmpty() || plan.get().getIsDeleted() == true) {
            throw new PlanNotFoundException("플랜을 찾을 수 없습니다.");
        }
    }

}
