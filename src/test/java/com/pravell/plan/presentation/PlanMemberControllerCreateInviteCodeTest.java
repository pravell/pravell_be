package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {
        "invite-code.characters=ABC123",
        "invite-code.length=6",
        "invite-code.expires=1"
})
class PlanMemberControllerCreateInviteCodeTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    @Autowired
    private PlanInviteCodeRepository planInviteCodeRepository;

    private final User owner = User.builder()
            .id(UUID.randomUUID())
            .userId("userId")
            .password("passworddd")
            .status(UserStatus.ACTIVE)
            .nickname("nickname")
            .build();

    private final User member = User.builder()
            .id(UUID.randomUUID())
            .userId("userId1")
            .password("passworddd")
            .status(UserStatus.ACTIVE)
            .nickname("nickname1")
            .build();

    private final User unMember = User.builder()
            .id(UUID.randomUUID())
            .userId("userId2")
            .password("passworddd")
            .status(UserStatus.ACTIVE)
            .nickname("nickname2")
            .build();

    private final User planWithdrawn = User.builder()
            .id(UUID.randomUUID())
            .userId("userIdadf2")
            .password("passworddd2")
            .status(UserStatus.ACTIVE)
            .nickname("nicknamesa")
            .build();

    private final Plan plan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isPublic(true)
            .isDeleted(false)
            .build();

    private final Plan deletedPlan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isPublic(true)
            .isDeleted(true)
            .build();

    private final PlanUsers planUsers1 = PlanUsers.builder()
            .planId(plan.getId())
            .userId(owner.getId())
            .planUserStatus(PlanUserStatus.OWNER)
            .build();

    private final PlanUsers planUsers2 = PlanUsers.builder()
            .planId(plan.getId())
            .userId(member.getId())
            .planUserStatus(PlanUserStatus.MEMBER)
            .build();

    private final PlanUsers planUsers3 = PlanUsers.builder()
            .planId(plan.getId())
            .userId(planWithdrawn.getId())
            .planUserStatus(PlanUserStatus.WITHDRAWN)
            .build();

    private final PlanUsers planUsers4 = PlanUsers.builder()
            .planId(deletedPlan.getId())
            .userId(owner.getId())
            .planUserStatus(PlanUserStatus.OWNER)
            .build();

    @BeforeEach
    void setUp() {
        userRepository.saveAll(List.of(owner, member, unMember, planWithdrawn));
        planRepository.saveAll(List.of(plan, deletedPlan));
        planUsersRepository.saveAll(List.of(planUsers1, planUsers2, planUsers3, planUsers4));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        planInviteCodeRepository.deleteAllInBatch();
    }

    @DisplayName("해당 플랜의 OWNER일 경우 초대 코드 생성에 성공한다.")
    @Test
    void shouldGenerateInviteCode_whenUserIsOwner() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String inviteCode = jsonNode.get("code").asText();

        assertNotNull(inviteCode);
        assertEquals(6, inviteCode.length());
        assertTrue(inviteCode.matches("[ABC123]{6}"));

        Optional<PlanInviteCode> planInviteCodes = planInviteCodeRepository.findByCode(inviteCode);
        assertThat(planInviteCodes).isPresent();
    }

    @DisplayName("해당 플랜의 MEMBER일 경우 초대 코드 생성에 성공한다.")
    @Test
    void shouldGenerateInviteCode_whenUserIsMember() throws Exception {
        //given
        String token = buildToken(member.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String inviteCode = jsonNode.get("code").asText();

        assertNotNull(inviteCode);
        assertEquals(6, inviteCode.length());
        assertTrue(inviteCode.matches("[ABC123]{6}"));

        Optional<PlanInviteCode> planInviteCodes = planInviteCodeRepository.findByCode(inviteCode);
        assertThat(planInviteCodes).isPresent();
    }

    @DisplayName("해당 플랜에 참여하지 않은 유저의 경우 초대 코드 생성에 실패한다.")
    @Test
    void shouldThrowAccessDenied_whenUnrelatedUserTriesToGenerateInviteCode() throws Exception {
        //given
        String token = buildToken(unMember.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜의 초대코드를 생성 할 권한이 없습니다."));

        assertThat(planInviteCodeRepository.count()).isZero();
    }

    @DisplayName("플랜에서 탈퇴한 유저의 경우 초대 코드 생성에 실패한다.")
    @Test
    void shouldThrowAccessDenied_whenWithdrawnUserTriesToGenerateInviteCode() throws Exception {
        //given
        String token = buildToken(planWithdrawn.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜의 초대코드를 생성 할 권한이 없습니다."));

        assertThat(planInviteCodeRepository.count()).isZero();
    }

    @DisplayName("플랜이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanDoesNotExist() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/" + UUID.randomUUID() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(planInviteCodeRepository.count()).isZero();
    }

    @DisplayName("이미 삭제된 플랜이면 초대코드 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenGeneratingInviteCodeFromDeletedPlan() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/" + deletedPlan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(planInviteCodeRepository.count()).isZero();
    }

    @DisplayName("유저가 존재하지 않으면 플랜 초대코드 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenGeneratingInviteCodeWithNonExistentUser() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(planInviteCodeRepository.count()).isZero();
    }

    @DisplayName("이미 탈퇴한 유저는 플랜 초대코드 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenGeneratingInviteCodeWithWithdrawnUser() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userIDdddddd")
                .password("passwordd")
                .nickname("qewrasdf")
                .status(UserStatus.WITHDRAWN)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/invite-code")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(planInviteCodeRepository.count()).isZero();
    }

}
