package com.pravell.plan.application;

import com.pravell.plan.application.dto.PlanMemberDTO;
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
import com.pravell.user.application.UserService;
import com.pravell.user.application.dto.UserMemberDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return buildCreatePlanResponse(planCreatedEvent);
    }

    private static CreatePlanResponse buildCreatePlanResponse(PlanCreatedEvent planCreatedEvent) {
        return CreatePlanResponse.builder()
                .planId(planCreatedEvent.getPlan().getId())
                .createdAt(planCreatedEvent.getCreatedAt())
                .isPublic(planCreatedEvent.getPlan().getIsPublic())
                .name(planCreatedEvent.getPlan().getName())
                .startDate(planCreatedEvent.getPlan().getStartDate())
                .endDate(planCreatedEvent.getPlan().getEndDate())
                .build();
    }

    public List<FindPlansResponse> findAllPlans(UUID id) {
        userService.findUserById(id);
        Map<UUID, List<Member>> planIdAndPlanMembers = buildPlanIdToMembersMap(id);
        return findPlanService.findAll(id, planIdAndPlanMembers);
    }

    private Map<UUID, List<Member>> buildPlanIdToMembersMap(UUID userId) {
        List<PlanUsers> planUsers = planService.findMemberOrOwnerPlanByUsers(userId);

        Map<UUID, List<Member>> planIdToMembers = new HashMap<>();

        for (PlanUsers planUser : planUsers) {
            UUID planId = planUser.getPlanId();

            List<UUID> activeMemberIds = getActiveMemberIds(planId);
            List<Member> members = getMembers(activeMemberIds);

            planIdToMembers.put(planId, members);
        }

        return planIdToMembers;
    }

    private List<UUID> getActiveMemberIds(UUID planId) {
        return planService.findPlanMembers(planId).stream()
                .filter(pm -> !pm.getPlanMemberStatus().equals("BLOCKED"))
                .map(PlanMemberDTO::getMemberId)
                .toList();
    }

    public FindPlanResponse findPlan(UUID planId, UUID userId) {
        userService.findUserById(userId);

        Plan plan = planService.findPlan(planId);
        List<PlanUsers> planUsers = planService.findPlanUsers(planId);

        findPlanService.validateMemberOrOwner(plan, userId, planUsers);

        List<UserMemberDTO> userMembers = getMembersFromPlanUsers(planUsers);
        UUID ownerId = extractOwnerId(planUsers);
        Pair<String, List<Member>> ownerAndMembers = separateOwnerAndMembers(ownerId, userMembers);

        return buildFindPlanResponse(plan, ownerId, ownerAndMembers);
    }

    private static FindPlanResponse buildFindPlanResponse(Plan plan, UUID ownerId,
                                                        Pair<String, List<Member>> ownerAndMembers) {
        return FindPlanResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .isPublic(plan.getIsPublic())
                .createdAt(plan.getCreatedAt())
                .ownerId(ownerId)
                .ownerNickname(ownerAndMembers.getFirst())
                .member(ownerAndMembers.getSecond())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
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
        return buildCreatePlanResponse(plan);
    }

    private static CreatePlanResponse buildCreatePlanResponse(Plan plan) {
        return CreatePlanResponse.builder()
                .planId(plan.getId())
                .name(plan.getName())
                .isPublic(plan.getIsPublic())
                .createdAt(plan.getCreatedAt())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .build();
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

    private List<Member> getMembers(List<UUID> memberId){
        List<UserMemberDTO> userMembers = userService.findMembers(memberId);

        return userMembers.stream().map(um->{
            return Member.builder()
                    .nickname(um.getNickname())
                    .memberId(um.getMemberId())
                    .build();
        }).toList();
    }

}
