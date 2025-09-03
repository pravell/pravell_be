package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.ControllerTestSupport;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("플랜 삭제 통합 테스트")
class PlanControllerDeleteTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    private final User owner = User.builder()
            .id(UUID.randomUUID())
            .userId("testId")
            .password("passwordd")
            .nickname("nickname")
            .status(UserStatus.ACTIVE)
            .build();


    private final User member = User.builder()
            .id(UUID.randomUUID())
            .userId("testId11")
            .password("passwordd")
            .nickname("nickname11")
            .status(UserStatus.ACTIVE)
            .build();


    private final Plan publicPlan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isDeleted(false)
            .isPublic(true)
            .build();


    private final PlanUsers planOwner = PlanUsers.builder()
            .planId(publicPlan.getId())
            .userId(owner.getId())
            .planUserStatus(PlanUserStatus.OWNER)
            .build();


    private final PlanUsers planMember = PlanUsers.builder()
            .planId(publicPlan.getId())
            .userId(member.getId())
            .planUserStatus(PlanUserStatus.MEMBER)
            .build();

    private final Plan privatePlan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isDeleted(false)
            .isPublic(false)
            .build();


    private final PlanUsers planOwner2 = PlanUsers.builder()
            .planId(privatePlan.getId())
            .userId(owner.getId())
            .planUserStatus(PlanUserStatus.OWNER)
            .build();


    private final PlanUsers planMember2 = PlanUsers.builder()
            .planId(privatePlan.getId())
            .userId(member.getId())
            .planUserStatus(PlanUserStatus.MEMBER)
            .build();


    @BeforeEach
    void setUp() {
        userRepository.saveAll(List.of(owner, member));
        planRepository.saveAll(List.of(publicPlan, privatePlan));
        planUsersRepository.saveAll(List.of(planMember, planOwner, planOwner2, planMember2));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("해당 플랜의 OWNER이고 PUBLIC 플랜일 경우, 플랜 삭제에 성공한다.")
    @Test
    void shouldDeletePublicPlanSuccessfully_whenUserIsOwner() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsDeleted()).isFalse();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsDeleted()).isTrue();
    }

    @DisplayName("해당 플랜의 OWNER이고 PRIVATE 플랜일 경우, 플랜 삭제에 성공한다.")
    @Test
    void shouldDeletePrivatePlanSuccessfully_whenUserIsOwner() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(privatePlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsDeleted()).isFalse();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + privatePlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        Optional<Plan> afterPlan = planRepository.findById(privatePlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsDeleted()).isTrue();
    }

    @DisplayName("해당 플랜의 MEMBER고, PUBLIC 플랜일 경우, 플랜 삭제에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403_whenPublicPlanDeleteAttemptedByMember() throws Exception {
        //given
        String token = buildToken(member.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsDeleted()).isFalse();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 삭제 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsDeleted()).isFalse();
    }

    @DisplayName("해당 플랜의 MEMBER고, PRIVATE 플랜일 경우, 플랜 삭제에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403_whenPrivatePlanDeleteAttemptedByMember() throws Exception {
        //given
        String token = buildToken(member.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(privatePlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsDeleted()).isFalse();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + privatePlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 삭제 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(privatePlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsDeleted()).isFalse();
    }

    @DisplayName("해당 플랜에 속해있지 않고, PUBLIC 플랜일 경우, 플랜 삭제에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403_whenNonMemberTriesToDeletePublicPlan() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId111")
                .password("passwordd")
                .status(UserStatus.ACTIVE)
                .nickname("nickname1111")
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsDeleted()).isFalse();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 삭제 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsDeleted()).isFalse();
    }

    @DisplayName("해당 플랜에 속해있지 않고, PRIVATE 플랜일 경우, 플랜 삭제에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403_whenNonMemberTriesToDeletePrivatePlan() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId111")
                .password("passwordd")
                .status(UserStatus.ACTIVE)
                .nickname("nickname1111")
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(privatePlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsDeleted()).isFalse();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + privatePlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 삭제 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(privatePlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsDeleted()).isFalse();
    }

    @DisplayName("플랜이 존재하지 않는 경우, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanDoesNotExist() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + UUID.randomUUID() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("플랜이 이미 삭제되었을 경우, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanIsAlreadyDeleted() throws Exception {
        //given
        Plan deletedPlan = Plan.builder()
                .id(UUID.randomUUID())
                .isDeleted(true)
                .isPublic(true)
                .name("플랜1")
                .build();
        planRepository.save(deletedPlan);

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + deletedPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("존재하지 않는 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("탈퇴한 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsWithdrawn() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId111")
                .password("passwordd")
                .status(UserStatus.WITHDRAWN)
                .nickname("nickname1111")
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("삭제된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsWithDeleted() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId111")
                .password("passwordd")
                .status(UserStatus.DELETED)
                .nickname("nickname1111")
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("정지된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsWithSuspended() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId111")
                .password("passwordd")
                .status(UserStatus.SUSPENDED)
                .nickname("nickname1111")
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("차단된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsWithBlocked() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId111")
                .password("passwordd")
                .status(UserStatus.BLOCKED)
                .nickname("nickname1111")
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + publicPlan.getId() + "/permanent")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

}
