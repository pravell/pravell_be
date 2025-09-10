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
import com.pravell.plan.presentation.request.WithdrawFromPlansRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlanMemberControllerWithdrawTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    @Autowired
    private PlanRepository planRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("MEMBER로 참여중인 플랜들에서 탈퇴가 가능하다.")
    @Test
    void shouldWithdrawFromPlan_whenUserIsMember() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId()))
                .build();

        //when, then
        mockMvc.perform(
                delete("/api/v1/plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.WITHDRAWN);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.WITHDRAWN);
    }

    @DisplayName("MEMBER로 참여중인 플랜들 중 삭제된 플랜이 있으면 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawFromDeletedPlanAsMember() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(true);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("존재하지 않는 플랜이 있으면 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenTryingToWithdrawFromNonExistentPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("참여중이지 않은 플랜이 있으면 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawFromNonExistentPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("소유하고 있는 플랜이 있으면 탈퇴에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenTryingToWithdrawFromOwnedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("플랜을 소유한 유저는 탈퇴할 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.OWNER);
    }

    @DisplayName("이미 탈퇴해서 참여중이지 않은 플랜이 있으면 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.WITHDRAWN);
    }

    @DisplayName("이미 퇴출당해서 참여중이지 않은 플랜이 있으면 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenKickedUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.KICKED);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.KICKED);
    }

    @DisplayName("이미 차단당해서 참여중이지 않은 플랜이 있으면 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.BLOCKED);
    }

    @DisplayName("이미 탈퇴한 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnAccountTriesToLeavePlan() throws Exception {
        //given
        User user = getUser(UserStatus.WITHDRAWN);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("이미 삭제된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedAccountTriesToLeavePlan() throws Exception {
        //given
        User user = getUser(UserStatus.DELETED);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("이미 정지된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedAccountTriesToLeavePlan() throws Exception {
        //given
        User user = getUser(UserStatus.SUSPENDED);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("이미 차단된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedAccountTriesToLeavePlan() throws Exception {
        //given
        User user = getUser(UserStatus.BLOCKED);
        userRepository.save(user);

        Plan plan1 = getPlan(false);
        Plan plan2 = getPlan(false);
        planRepository.saveAll(List.of(plan1, plan2));

        PlanUsers planUsers1 = getPlanUsers(plan1.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan2.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        WithdrawFromPlansRequest request = WithdrawFromPlansRequest.builder()
                .planIds(List.of(plan1.getId(), plan2.getId(), UUID.randomUUID()))
                .build();

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> afterPlan1 = planUsersRepository.findByPlanIdAndUserId(plan1.getId(), user.getId());
        assertThat(afterPlan1).isPresent();
        assertThat(afterPlan1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> afterPlan2 = planUsersRepository.findByPlanIdAndUserId(plan2.getId(), user.getId());
        assertThat(afterPlan2).isPresent();
        assertThat(afterPlan2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    private User getUser(UserStatus userStatus) {
        return User.builder()
                .id(UUID.randomUUID())
                .userId("userId" + UUID.randomUUID())
                .password("passworddd")
                .nickname("nickname" + UUID.randomUUID())
                .status(userStatus)
                .build();
    }

    private Plan getPlan(boolean isDeleted){
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("경주여행")
                .isPublic(true)
                .isDeleted(isDeleted)
                .startDate(LocalDate.parse("2025-09-29"))
                .endDate(LocalDate.parse("2025-09-30"))
                .build();
    }

    private PlanUsers getPlanUsers(UUID planId, UUID userId, PlanUserStatus planUserStatus){
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(planUserStatus)
                .build();
    }

}
