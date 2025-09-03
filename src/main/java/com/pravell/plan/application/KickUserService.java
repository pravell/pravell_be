package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.application.dto.request.KickUsersFromPlanApplicationRequest;
import com.pravell.plan.domain.exception.PlanNotFoundException;
import com.pravell.plan.domain.model.PlanUsers;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KickUserService {

    @Transactional
    public void kickUsers(UUID id, List<PlanUsers> planUsers, KickUsersFromPlanApplicationRequest request) {
        Map<UUID, PlanUsers> planUsersMap = planUsers.stream()
                .collect(Collectors.toMap(PlanUsers::getUserId, pu -> pu));

        for (UUID kickUserId : request.getDeleteUsers()){
            if (id.equals(kickUserId)){
                throw new IllegalArgumentException("본인은 삭제할 수 없습니다.");
            }

            PlanUsers user = planUsersMap.get(kickUserId);
            if (user == null) {
                throw new PlanNotFoundException("해당 플랜에 유저가 존재하지 않습니다.");
            }


            switch(user.getPlanUserStatus()) {
                case MEMBER -> {
                    log.info("{} 유저가 {} 유저를 {} 플랜에서 퇴출시켰습니다.", id, kickUserId, user.getPlanId());
                    user.updateToKicked();
                }
                case OWNER -> throw new AccessDeniedException("플랜을 소유한 유저는 퇴출시킬 수 없습니다.");
                default -> throw new PlanNotFoundException("해당 플랜에 유저가 존재하지 않습니다.");
            }
        }
    }

}
