package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.domain.exception.CodeNotFoundException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanInviteCode;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanInviteCodeRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class JoinPlanService {

    private final PlanInviteCodeRepository planInviteCodeRepository;
    private final PlanUsersRepository planUsersRepository;

    @Transactional(readOnly = true)
    public PlanInviteCode findPlanInviteCode(String code) {
        PlanInviteCode inviteCode = planInviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new CodeNotFoundException("올바른 초대코드가 아닙니다."));

        if (inviteCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CodeNotFoundException("초대 코드가 만료되었습니다.");
        }

        return inviteCode;
    }

    @Transactional
    public void join(UUID userId, Plan plan) {
        Optional<PlanUsers> existsPlanUser = planUsersRepository.findByPlanIdAndUserId(plan.getId(), userId);
        if (existsPlanUser.isEmpty()) {
            log.info("{} 유저가 {} 플랜에 가입하였습니다.", userId, plan.getId());
            planUsersRepository.save(PlanUsers.builder()
                    .planId(plan.getId())
                    .userId(userId)
                    .planUserStatus(PlanUserStatus.MEMBER)
                    .build());
        } else if (existsPlanUser.get().getPlanUserStatus().equals(PlanUserStatus.MEMBER) ||
                existsPlanUser.get().getPlanUserStatus().equals(PlanUserStatus.OWNER)) {
            log.info("이미 플랜에 참여중인 유저입니다. PlanId : {}, User Id : {}", plan.getId(), userId);
            throw new IllegalArgumentException("이미 플랜에 참여중인 유저입니다.");
        } else if (existsPlanUser.get().getPlanUserStatus().equals(PlanUserStatus.BLOCKED)) {
            log.info("해당 플랜에 참여가 불가능합니다. PlanId : {}, User Id : {}", plan.getId(), userId);
            throw new AccessDeniedException("해당 플랜에 참여가 불가능합니다.");
        } else {
            log.info("{} 유저를 {} 플랜에 다시 가입시켰습니다. beforeStatus : {}", userId, plan.getId(),
                    existsPlanUser.get().getPlanUserStatus());
            existsPlanUser.get().updateToMember();
        }
    }

}
