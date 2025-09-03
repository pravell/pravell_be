package com.pravell.plan.application;

import com.pravell.plan.domain.exception.PlanNotFoundException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanUsersRepository planUsersRepository;

    @Transactional(readOnly = true)
    public Plan findPlan(UUID planId) {
        Optional<Plan> plan = planRepository.findById(planId);
        if (plan.isEmpty() || plan.get().getIsDeleted() == true) {
            throw new PlanNotFoundException("플랜을 찾을 수 없습니다.");
        }

        return plan.get();
    }

    @Transactional(readOnly = true)
    public List<PlanUsers> findPlanUsers(UUID planId) {
        return planUsersRepository.findAllByPlanId(planId);
    }

}
