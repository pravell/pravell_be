package com.pravell.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.application.dto.request.UpdatePlanApplicationRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.service.PlanAuthorizationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdatePlanServiceTest {

    private UpdatePlanService updatePlanService;
    private UUID ownerId;
    private UUID memberId;
    private Plan plan;

    @BeforeEach
    void setUp() {
        updatePlanService = new UpdatePlanService(new PlanAuthorizationService());
        ownerId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        plan = Plan.builder()
                .id(UUID.randomUUID())
                .name("기존 이름")
                .isPublic(false)
                .build();
    }

    @DisplayName("OWNER가 이름과 공개 여부를 수정할 수 있다.")
    @Test
    void shouldUpdatePlanSuccessfully_whenUserIsOwner() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .userId(ownerId)
                        .planUserStatus(PlanUserStatus.OWNER)
                        .build()
        );

        UpdatePlanApplicationRequest request = UpdatePlanApplicationRequest.builder()
                .name("새로운 이름")
                .isPublic(true)
                .build();

        //when
        updatePlanService.update(plan, ownerId, planUsers, request);

        //then
        assertThat(plan.getName()).isEqualTo("새로운 이름");
        assertThat(plan.getIsPublic()).isTrue();
    }

    @DisplayName("MEMBER는 수정 권한이 없어 예외가 발생한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserIsNotOwner() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .userId(memberId)
                        .planUserStatus(PlanUserStatus.MEMBER)
                        .build()
        );

        UpdatePlanApplicationRequest request = UpdatePlanApplicationRequest.builder()
                .name("수정할이름")
                .isPublic(true)
                .build();

        //when, then
        assertThatThrownBy(() -> updatePlanService.update(plan, memberId, planUsers, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 리소스를 수정 할 권한이 없습니다.");
    }

    @DisplayName("이름만 수정 요청한 경우 이름만 변경된다.")
    @Test
    void shouldUpdateOnlyName_whenOnlyNameProvided() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .userId(ownerId)
                        .planUserStatus(PlanUserStatus.OWNER)
                        .build()
        );

        UpdatePlanApplicationRequest request = UpdatePlanApplicationRequest.builder()
                .name("이름만 변경")
                .build();

        //when
        updatePlanService.update(plan, ownerId, planUsers, request);

        //then
        assertThat(plan.getName()).isEqualTo("이름만 변경");
        assertThat(plan.getIsPublic()).isFalse();
    }

    @DisplayName("공개 여부만 수정 요청한 경우 공개 여부만 변경된다.")
    @Test
    void shouldUpdateOnlyVisibility_whenOnlyVisibilityProvided() {
        //given
        List<PlanUsers> planUsers = List.of(
                PlanUsers.builder()
                        .userId(ownerId)
                        .planUserStatus(PlanUserStatus.OWNER)
                        .build()
        );

        UpdatePlanApplicationRequest request = UpdatePlanApplicationRequest.builder()
                .isPublic(true)
                .build();

        //when
        updatePlanService.update(plan, ownerId, planUsers, request);

        //then
        assertThat(plan.getIsPublic()).isTrue();
        assertThat(plan.getName()).isEqualTo("기존 이름");
    }

}
