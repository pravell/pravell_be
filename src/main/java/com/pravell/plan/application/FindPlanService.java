package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.application.dto.response.FindPlansResponse;
import com.pravell.plan.domain.model.Member;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindPlanService {

    private final PlanRepository planRepository;
    private final PlanUsersRepository planUsersRepository;

    @Transactional(readOnly = true)
    public List<FindPlansResponse> findAll(UUID userId, Map<UUID, List<Member>> planIdAndPlanMembers) {
        List<UUID> planIds = new ArrayList<>(planIdAndPlanMembers.keySet());
        List<Plan> plans = planRepository.findAllByIdIn(planIds);

        Map<UUID, Plan> planMap = plans.stream()
                .filter(p -> p.getIsDeleted().equals(false))
                .collect(Collectors.toMap(Plan::getId, Function.identity()));

        List<PlanUsers> planUsersList = planUsersRepository.findAllByUserId(userId);
        Map<UUID, PlanUsers> planUserMap = planUsersList.stream()
                .collect(Collectors.toMap(PlanUsers::getPlanId, Function.identity()));

        return planIdAndPlanMembers.entrySet().stream()
                .map(entry -> {
                    UUID planId = entry.getKey();
                    List<Member> members = entry.getValue();
                    Plan plan = planMap.get(planId);
                    PlanUsers planUser = planUserMap.get(planId);

                    if (plan == null || planUser == null) {
                        return null;
                    }

                    return FindPlansResponse.builder()
                            .planId(plan.getId())
                            .planName(plan.getName())
                            .isOwner(planUser.getPlanUserStatus().equals(PlanUserStatus.OWNER))
                            .members(members.stream()
                                    .map(Member::getNickname)
                                    .toList())
                            .startDate(plan.getStartDate())
                            .endDate(plan.getEndDate())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public void validateMemberOrOwner(Plan plan, UUID userId, List<PlanUsers> planUsers) {
        boolean isBlocked = planUsers.stream()
                .anyMatch(p -> p.getUserId().equals(userId) && p.getPlanUserStatus().equals(PlanUserStatus.BLOCKED));

        if (isBlocked) {
            throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
        }

        if (!plan.getIsPublic()) {
            boolean isMemberOrOwner = planUsers.stream()
                    .filter(p ->
                            p.getPlanUserStatus().equals(PlanUserStatus.OWNER) ||
                                    p.getPlanUserStatus().equals(PlanUserStatus.MEMBER))
                    .map(PlanUsers::getUserId)
                    .anyMatch(pu -> pu.equals(userId));

            if (!isMemberOrOwner) {
                throw new AccessDeniedException("해당 리소스에 접근 할 권한이 없습니다.");
            }
        }
    }

}
