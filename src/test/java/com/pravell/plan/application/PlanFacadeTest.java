package com.pravell.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
import com.pravell.plan.application.dto.response.CreatePlanResponse;
import com.pravell.plan.domain.event.PlanCreatedEvent;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.application.UserService;
import com.pravell.user.domain.exception.UserNotFoundException;
import com.pravell.user.domain.model.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanFacadeTest {

    @Mock
    private CreatePlanService createPlanService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PlanFacade planFacade;

    @DisplayName("유효한 유저와 요청으로 플랜을 생성하면 응답을 반환한다.")
    @Test
    void shouldCreatePlan_whenValidUserAndRequest() {
        //given
        UUID userId = UUID.randomUUID();

        CreatePlanApplicationRequest request = CreatePlanApplicationRequest.builder()
                .name("플랜 1")
                .isPublic(true)
                .build();

        Plan plan = Plan.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .isPublic(request.getIsPublic())
                .build();

        PlanUsers planUsers = PlanUsers.builder()
                .id(1L)
                .planId(plan.getId())
                .userId(userId)
                .planUserStatus(PlanUserStatus.OWNER)
                .build();

        PlanCreatedEvent planCreatedEvent = new PlanCreatedEvent(plan, planUsers, LocalDateTime.now());

        given(userService.findUserById(userId)).willReturn(mock(User.class));
        given(createPlanService.create(request, userId)).willReturn(planCreatedEvent);

        //when
        CreatePlanResponse response = planFacade.createPlan(request, userId);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getPlanId()).isEqualTo(plan.getId());
        assertThat(response.getIsPublic()).isEqualTo(true);
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getCreatedAt()).isEqualTo(planCreatedEvent.getCreatedAt());

        then(userService).should().findUserById(userId);
        then(createPlanService).should().create(request, userId);
    }

    @DisplayName("유저가 존재하지 않으면 플랜 생성에 실패하고 예외가 발생한다.")
    @Test
    void shouldThrow_whenUserNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        CreatePlanApplicationRequest request = CreatePlanApplicationRequest.builder()
                .name("부산 여행")
                .isPublic(false)
                .build();

        given(userService.findUserById(userId)).willThrow(new UserNotFoundException("유저를 찾을 수 없습니다."));

        // when, then
        assertThatThrownBy(() -> planFacade.createPlan(request, userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저를 찾을 수 없습니다.");

        then(createPlanService).should(never()).create(any(), any());
    }

}

