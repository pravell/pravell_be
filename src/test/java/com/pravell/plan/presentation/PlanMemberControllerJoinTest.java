package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.ControllerTestSupport;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanInviteCode;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanInviteCodeRepository;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlanMemberControllerJoinTest extends ControllerTestSupport {

    @Autowired
    private PlanInviteCodeRepository planInviteCodeRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        planInviteCodeRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("초대 코드로 PUBLIC 플랜에 참여한다.")
    @Test
    void shouldJoinPublicPlan_whenInviteCodeIsValid() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(plan.getId().toString()));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("초대 코드로 PRIVATE 플랜에 참여한다.")
    @Test
    void shouldJoinPrivatePlan_whenInviteCodeIsValid() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(plan.getId().toString()));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("탈퇴한 유저는 플랜에 재가입이 가능하다.")
    @Test
    void shouldRejoinPlan_whenWithdrawnUserUsesValidInviteCode() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<PlanUsers> before = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.WITHDRAWN);

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(plan.getId().toString()));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("퇴출당한 유저는 플랜에 재가입이 가능하다.")
    @Test
    void shouldRejoinPlan_whenKickedUserUsesValidInviteCode() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.KICKED);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<PlanUsers> before = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.KICKED);

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(plan.getId().toString()));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("차단당한 유저는 플랜에 재가입이 불가능하며, 403을 반환한다.")
    @Test
    void shouldThrowAccessDenied_whenBlockedUserTriesToJoinPlanWithValidInviteCode() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<PlanUsers> before = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.BLOCKED);

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 참여가 불가능합니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.BLOCKED);
    }

    @DisplayName("초대코드가 만료되었으면 플랜에 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenInviteCodeIsExpired() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("초대 코드가 만료되었습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("초대코드가 올바르지 않으면 플랜에 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenInviteCodeIsInvalid() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", "acbdafde"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("올바른 초대코드가 아닙니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("삭제된 플랜에는 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenJoiningDeletedPlanWithInviteCode() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("탈퇴한 유저는 플랜에 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnUserTriesToJoinPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.WITHDRAWN);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("삭제된 유저는 플랜에 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedUserTriesToJoinPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.DELETED);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("정지된 유저는 플랜에 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedUserTriesToJoinPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.SUSPENDED);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("차단당한 유저는 플랜에 참여가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedUserTriesToJoinPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.BLOCKED);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("만료된 토큰으로는 플랜에 참여가 불가능하며, 401을 반환한다.")
    @Test
    void shouldReturn401_whenExpiredTokenUsedToJoinPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    @DisplayName("올바르지 않은 토큰으로는 플랜에 참여가 불가능하며, 401을 반환한다.")
    @Test
    void shouldReturn401_whenInvalidTokenUsedToJoinPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User user = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, owner));

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        planUsersRepository.saveAll(List.of(planUsers1));

        PlanInviteCode planInviteCode = PlanInviteCode.builder()
                .code("abc111")
                .planId(plan.getId())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdBy(owner.getId())
                .build();
        planInviteCodeRepository.save(planInviteCode);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000)) + "Aaa";

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/join")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .queryParam("code", planInviteCode.getCode()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        Optional<PlanUsers> after = planUsersRepository.findByPlanIdAndUserId(plan.getId(), user.getId());
        assertThat(after).isNotPresent();
    }

    private User getUser(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .nickname("nickname" + UUID.randomUUID())
                .userId("userId" + UUID.randomUUID())
                .status(status)
                .password("passwrddd")
                .build();
    }

    private Plan getPlan(boolean idDeleted, boolean isPublic) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("name" + UUID.randomUUID())
                .isDeleted(idDeleted)
                .isPublic(isPublic)
                .build();
    }

    private PlanUsers getPlanUsers(UUID planId, UUID userId, PlanUserStatus status) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

}
