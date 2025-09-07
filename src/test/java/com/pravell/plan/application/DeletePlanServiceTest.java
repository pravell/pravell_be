package com.pravell.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.service.PlanAuthorizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeletePlanServiceTest {

    private final DeletePlanService deletePlanService = new DeletePlanService(new PlanAuthorizationService());

    private final UUID ownerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID unrelatedUserId = UUID.randomUUID();
    private final Plan plan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isDeleted(false)
            .isPublic(false)
            .build();

    @Test
    @DisplayName("OWNER가 플랜 삭제 시, 정상적으로 삭제된다.")
    void shouldDeletePlanSuccessfully_whenUserIsOwner() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .planId(plan.getId())
                        .planUserStatus(PlanUserStatus.OWNER)
                        .userId(ownerId)
                        .build(),
                PlanUsers.builder()
                        .planId(plan.getId())
                        .planUserStatus(PlanUserStatus.MEMBER)
                        .userId(memberId)
                        .build());

        //when
        deletePlanService.deletePlan(plan, ownerId, planUsers);

        //then
        assertThat(plan.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("MEMBER가 삭제 시도 시, 예외가 발생한다.")
    void shouldThrowAccessDenied_whenUserIsNotOwner() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .planId(plan.getId())
                        .planUserStatus(PlanUserStatus.OWNER)
                        .userId(ownerId)
                        .build(),
                PlanUsers.builder()
                        .planId(plan.getId())
                        .planUserStatus(PlanUserStatus.MEMBER)
                        .userId(memberId)
                        .build());

        //when, then
        assertThatThrownBy(()->deletePlanService.deletePlan(plan, memberId, planUsers))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 리소스를 삭제 할 권한이 없습니다.");

        assertThat(plan.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("플랜에 속하지 않은 유저가 삭제 시도 시, 예외가 발생한다.")
    void shouldThrowAccessDenied_whenUserNotInPlanUsers() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .planId(plan.getId())
                        .planUserStatus(PlanUserStatus.OWNER)
                        .userId(ownerId)
                        .build(),
                PlanUsers.builder()
                        .planId(plan.getId())
                        .planUserStatus(PlanUserStatus.MEMBER)
                        .userId(memberId)
                        .build());

        //when, then
        assertThatThrownBy(()->deletePlanService.deletePlan(plan, unrelatedUserId, planUsers))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 리소스를 삭제 할 권한이 없습니다.");

        assertThat(plan.getIsDeleted()).isFalse();
    }

}
