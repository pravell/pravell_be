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
import com.pravell.plan.presentation.request.KickUsersFromPlanRequest;
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

class PlanMemberControllerKickedTest extends ControllerTestSupport {

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

    @DisplayName("플랜을 소유한 유저는 플랜 멤버들을 퇴출시킬 수 있다.")
    @Test
    void shouldKickOutMember_whenUserIsOwnerOfPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(List.of(member1.getId(), member2.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.KICKED);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.KICKED);
    }

    @DisplayName("플랜을 소유한 유저가 아니라면 플랜 멤버들을 퇴출시킬 수 없고, 403을 반환한다.")
    @Test
    void shouldThrow403_whenUserIsNotOwnerOfPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(member2.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(List.of(member1.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("본인은 삭제할 수 없고, 400을 반환한다.")
    @Test
    void shouldThrow400_whenUserTriesToKickThemself() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(
                List.of(owner.getId(), member1.getId(), member2.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("본인은 삭제할 수 없습니다."));

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("플랜에 존재하지 않는 멤버를 퇴출시키려고 하면, 퇴출에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenTryingToKickNonexistentMember() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        User member3 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2, member3));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(
                List.of(member1.getId(), member2.getId(), member3.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
    }

    @DisplayName("탈퇴한 멤버를 퇴출시키려고 하면, 퇴출에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenKickingAlreadyWithdrawnMemberFromPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(List.of(member1.getId(), member2.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.WITHDRAWN);
    }

    @DisplayName("퇴출당한 멤버를 퇴출시키려고 하면, 퇴출에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenKickingAlreadyKickedMemberFromPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.KICKED);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(List.of(member1.getId(), member2.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.KICKED);
    }

    @DisplayName("블락당한 멤버를 퇴출시키려고 하면, 퇴출에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenKickingBlockedMemberFromPlan() throws Exception {
        //given
        User owner = getUser(UserStatus.ACTIVE);
        User member1 = getUser(UserStatus.ACTIVE);
        User member2 = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(owner, member1, member2));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers1 = getPlanUsers(plan.getId(), owner.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), member1.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), member2.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3));

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        KickUsersFromPlanRequest request = new KickUsersFromPlanRequest(List.of(member1.getId(), member2.getId()));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/plans/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 유저가 존재하지 않습니다."));

        Optional<PlanUsers> after1 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers2.getUserId());
        assertThat(after1).isPresent();
        assertThat(after1.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.MEMBER);
        Optional<PlanUsers> after2 = planUsersRepository.findByPlanIdAndUserId(plan.getId(), planUsers3.getUserId());
        assertThat(after2).isPresent();
        assertThat(after2.get().getPlanUserStatus()).isEqualTo(PlanUserStatus.BLOCKED);
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

    private Plan getPlan(boolean isDeleted) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("경주여행")
                .isPublic(true)
                .isDeleted(isDeleted)
                .startDate(LocalDate.parse("2025-09-29"))
                .endDate(LocalDate.parse("2025-09-30"))
                .build();
    }

    private PlanUsers getPlanUsers(UUID planId, UUID userId, PlanUserStatus planUserStatus) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(planUserStatus)
                .build();
    }

}
