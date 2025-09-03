package com.pravell.plan.application;

import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
import com.pravell.plan.application.dto.request.UpdatePlanApplicationRequest;
import com.pravell.plan.application.dto.response.CreatePlanResponse;
import com.pravell.plan.application.dto.response.FindPlanResponse;
import com.pravell.plan.application.dto.response.FindPlansResponse;
import com.pravell.plan.domain.event.PlanCreatedEvent;
import com.pravell.plan.domain.model.Member;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.presentation.request.UpdatePlanRequest;
import com.pravell.user.application.UserService;
import com.pravell.user.application.dto.UserMemberDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanFacade {

    private final UserService userService;
    private final CreatePlanService createPlanService;
    private final FindPlanService findPlanService;
    private final PlanService planService;
    private final DeletePlanService deletePlanService;
    private final UpdatePlanService updatePlanService;

    public CreatePlanResponse createPlan(CreatePlanApplicationRequest request, UUID id) {
        userService.findUserById(id);

        PlanCreatedEvent planCreatedEvent = createPlanService.create(request, id);

        return CreatePlanResponse.builder()
                .planId(planCreatedEvent.getPlan().getId())
                .createdAt(planCreatedEvent.getCreatedAt())
                .isPublic(planCreatedEvent.getPlan().getIsPublic())
                .name(planCreatedEvent.getPlan().getName())
                .build();
    }

    public List<FindPlansResponse> findAllPlans(UUID id) {
        userService.findUserById(id);
        return findPlanService.findAll(id);
    }

    public FindPlanResponse findPlan(UUID planId, UUID userId) {
        userService.findUserById(userId);

        Plan plan = planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        findPlanService.validateMemberOrOwner(plan, userId, planUsers);

        List<UserMemberDTO> userMembers = getMembersFromPlanUsers(planUsers);
        UUID ownerId = extractOwnerId(planUsers);
        Pair<String, List<Member>> ownerAndMembers = separateOwnerAndMembers(ownerId, userMembers);

        return FindPlanResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .isPublic(plan.getIsPublic())
                .createdAt(plan.getCreatedAt())
                .ownerId(ownerId)
                .ownerNickname(ownerAndMembers.getFirst())
                .member(ownerAndMembers.getSecond())
                .build();
    }

    private List<UserMemberDTO> getMembersFromPlanUsers(List<PlanUsers> planUsers) {
        List<UUID> memberIds = planUsers.stream()
                .filter(pu -> pu.getPlanUserStatus().equals(PlanUserStatus.MEMBER) ||
                        pu.getPlanUserStatus().equals(PlanUserStatus.OWNER))
                .map(PlanUsers::getUserId)
                .toList();

        return userService.findMembers(memberIds);
    }

    private UUID extractOwnerId(List<PlanUsers> planUsers) {
        return planUsers.stream().filter(pu -> pu.getPlanUserStatus().equals(PlanUserStatus.OWNER))
                .map(PlanUsers::getUserId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OWNER가 존재하지 않습니다."));
    }

    private Pair<String, List<Member>> separateOwnerAndMembers(UUID ownerId, List<UserMemberDTO> userMembers) {
        String ownerNickname = null;
        List<Member> members = new ArrayList<>();

        for (UserMemberDTO um : userMembers) {
            if (um.getMemberId().equals(ownerId)) {
                ownerNickname = um.getNickname();
            } else {
                members.add(Member.builder()
                        .memberId(um.getMemberId())
                        .nickname(um.getNickname())
                        .build());
            }
        }

        return Pair.of(ownerNickname, members);
    }

    public void deletePlan(UUID planId, UUID userId) {
        userService.findUserById(userId);

        Plan plan = planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        deletePlanService.deletePlan(plan, userId, planUsers);
    }

    public CreatePlanResponse updatePlan(UUID planId, UUID userId, UpdatePlanApplicationRequest request) {
        userService.findUserById(userId);

        Plan plan = planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        updatePlanService.update(plan, userId, planUsers, request);

        Plan afterPlan = planService.findPlan(planId);
        return CreatePlanResponse.builder()
                .planId(afterPlan.getId())
                .name(afterPlan.getName())
                .isPublic(afterPlan.getIsPublic())
                .createdAt(afterPlan.getCreatedAt())
                .build();
    }

}
