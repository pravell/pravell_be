package com.pravell.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.groups.Tuple.tuple;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.application.dto.response.FindPlansResponse;
import com.pravell.plan.domain.model.Member;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class FindPlanServiceTest {

    @Autowired
    private FindPlanService findPlanService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("해당 유저가 참여중이고, 삭제되지 않은 플랜들을 조회한다.")
    @Test
    void shouldRetrievePlansUserIsParticipatingIn() {
        //given
        UUID userId = UUID.randomUUID();

        Plan plan1 = getPlan("정상 조회 플랜1", true, false);
        PlanUsers planUsers1 = getPlanUsers(PlanUserStatus.OWNER, plan1.getId(), userId);
        Plan plan2 = getPlan("정상 조회 플랜2", false, false);
        PlanUsers planUsers2 = getPlanUsers(PlanUserStatus.OWNER, plan2.getId(), userId);
        Plan plan3 = getPlan("정상 조회 플랜3", true, false);
        PlanUsers planUsers3 = getPlanUsers(PlanUserStatus.MEMBER, plan3.getId(), userId);

        Plan plan4 = getPlan("삭제된 플랜1", true, true);
        PlanUsers planUsers4 = getPlanUsers(PlanUserStatus.MEMBER, plan4.getId(), userId);
        Plan plan5 = getPlan("탈퇴한 플랜1", true, false);
        PlanUsers planUsers5 = getPlanUsers(PlanUserStatus.WITHDRAWN, plan5.getId(), userId);
        Plan plan6 = getPlan("강퇴당한 플랜1", true, false);
        PlanUsers planUsers6 = getPlanUsers(PlanUserStatus.KICKED, plan6.getId(), userId);
        Plan plan7 = getPlan("차단당한 플랜1", true, false);
        PlanUsers planUsers7 = getPlanUsers(PlanUserStatus.KICKED, plan7.getId(), userId);

        planRepository.saveAll(List.of(plan1, plan2, plan3, plan4, plan5, plan6, plan7));
        planUsersRepository.saveAll(
                List.of(planUsers1, planUsers2, planUsers3, planUsers4, planUsers5, planUsers6, planUsers7));

        Map<UUID, List<Member>> planIdAndPlanMembers = new HashMap<>();

        planIdAndPlanMembers.put(plan1.getId(), List.of(getMember("멤버 1"), getMember("멤버 2")));
        planIdAndPlanMembers.put(plan2.getId(), List.of(getMember("멤버1")));
        planIdAndPlanMembers.put(plan3.getId(), List.of(getMember("멤버4"), getMember("멤버5")));

        //when
        List<FindPlansResponse> responses = findPlanService.findAll(userId, planIdAndPlanMembers);

        //then
        assertThat(responses).hasSize(3)
                .extracting("planId", "planName", "isOwner")
                .containsExactlyInAnyOrder(
                        tuple(plan1.getId(), plan1.getName(), true),
                        tuple(plan2.getId(), plan2.getName(), true),
                        tuple(plan3.getId(), plan3.getName(), false)
                );
    }

    @Test
    @DisplayName("BLOCKED 멤버면 접근할 수 없다.")
    void shouldThrowAccessDenied_whenUserIsBlocked() {
        //given
        Plan plan = getPlan(true);
        List<PlanUsers> planUsers = List.of(getPlanUser(userId, PlanUserStatus.BLOCKED));

        //when, then
        assertThatThrownBy(() -> findPlanService.validateMemberOrOwner(plan, userId, planUsers))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 리소스에 접근 할 권한이 없습니다.");
    }

    @Test
    @DisplayName("PRIVATE 플랜의 OWNER 또는 MEMBER가 아니면 접근할 수 없다.")
    void shouldThrowAccessDenied_whenUserNotMemberOrOwnerInPrivatePlan() {
        //given
        Plan plan = getPlan(false);
        UUID otherUserId = UUID.randomUUID();
        List<PlanUsers> planUsers = List.of(
                getPlanUser(otherUserId, PlanUserStatus.MEMBER)
        );

        //when, then
        assertThatThrownBy(() -> findPlanService.validateMemberOrOwner(plan, userId, planUsers))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 리소스에 접근 할 권한이 없습니다.");
    }

    @Test
    @DisplayName("PRIVATE 플랜의 MEMBER면 접근 가능하다.")
    void shouldPass_whenUserIsMemberInPrivatePlan() {
        //given
        Plan plan = getPlan(false);
        List<PlanUsers> planUsers = List.of(getPlanUser(userId, PlanUserStatus.MEMBER));

        //when, then
        assertThatCode(() -> findPlanService.validateMemberOrOwner(plan, userId, planUsers))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PRIVATE 플랜의 OWNER면 접근 가능하다.")
    void shouldPass_whenUserIsOwnerInPrivatePlan() {
        //given
        Plan plan = getPlan(false);
        List<PlanUsers> planUsers = List.of(getPlanUser(userId, PlanUserStatus.OWNER));

        //when, then
        assertThatCode(() -> findPlanService.validateMemberOrOwner(plan, userId, planUsers))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PUBLIC 플랜은 OWNER/MEMBER가 아니어도 BLOCKED가 아니면 접근 가능하다.")
    void shouldPass_whenUserIsNotMemberButPlanIsPublic() {
        //given
        Plan plan = getPlan(true);
        List<PlanUsers> planUsers = List.of(
                getPlanUser(UUID.randomUUID(), PlanUserStatus.MEMBER)
        );

        //when, then
        assertThatCode(() -> findPlanService.validateMemberOrOwner(plan, userId, planUsers))
                .doesNotThrowAnyException();
    }

    private Plan getPlan(String name, boolean isPublic, boolean isDeleted) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isPublic(isPublic)
                .isDeleted(isDeleted)
                .startDate(LocalDate.parse("2025-09-29"))
                .endDate(LocalDate.parse("2025-09-30"))
                .build();
    }

    private PlanUsers getPlanUsers(PlanUserStatus status, UUID planId, UUID userId) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

    private Plan getPlan(boolean isPublic) {
        return Plan.builder()
                .id(planId)
                .name("테스트 여행")
                .isDeleted(false)
                .startDate(LocalDate.parse("2025-09-29"))
                .endDate(LocalDate.parse("2025-09-30"))
                .isPublic(isPublic)
                .build();
    }

    private PlanUsers getPlanUser(UUID userId, PlanUserStatus status) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

    private Member getMember(String nickname) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .nickname(nickname)
                .build();
    }

}