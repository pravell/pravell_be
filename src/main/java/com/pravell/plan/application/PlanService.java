package com.pravell.plan.application;

import com.pravell.plan.application.dto.PlanMemberDTO;
import com.pravell.plan.domain.exception.PlanNotFoundException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
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

    @Transactional(readOnly = true)
    public List<PlanMemberDTO> findPlanMembers(UUID planId) {
        List<PlanUsers> planUsers = planUsersRepository.findAllByPlanId(planId);

        return planUsers.stream().filter(pu -> pu.getPlanUserStatus().equals(PlanUserStatus.MEMBER) ||
                        pu.getPlanUserStatus().equals(PlanUserStatus.OWNER) ||
                        pu.getPlanUserStatus().equals(PlanUserStatus.BLOCKED))
                .map(p -> {
                    return PlanMemberDTO.builder()
                            .memberId(p.getUserId())
                            .planMemberStatus(p.getPlanUserStatus().name())
                            .build();
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<PlanMemberDTO> findActivePlanMembers(UUID planId) {
        List<PlanUsers> planUsers = planUsersRepository.findAllByPlanId(planId);

        return planUsers.stream().filter(pu -> pu.getPlanUserStatus().equals(PlanUserStatus.MEMBER) ||
                        pu.getPlanUserStatus().equals(PlanUserStatus.OWNER))
                .map(p -> {
                    return PlanMemberDTO.builder()
                            .memberId(p.getUserId())
                            .build();
                }).toList();
    }

    @Transactional(readOnly = true)
    public boolean isPlanPublic(UUID planId) {
        Optional<Plan> plan = planRepository.findById(planId);
        if (plan.isEmpty() || plan.get().getIsDeleted() == true) {
            throw new PlanNotFoundException("플랜을 찾을 수 없습니다.");
        }

        return plan.get().getIsPublic();
    }

    @Transactional(readOnly = true)
    public List<PlanUsers> findMemberOrOwnerPlanByUsers(UUID id) {
        List<PlanUsers> planUsers = planUsersRepository.findAllByUserId(id);

        return planUsers.stream()
                .filter(pu -> pu.getPlanUserStatus().equals(PlanUserStatus.MEMBER) ||
                        pu.getPlanUserStatus().equals(PlanUserStatus.OWNER))
                .toList();
    }

}
