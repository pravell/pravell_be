package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("플랜 상세 조회 통합 테스트")
class PlanControllerFindTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    @Autowired
    private PlanRepository planRepository;

    private static final UUID activePlanId = UUID.randomUUID();
    private static final UUID privatePlanId = UUID.randomUUID();
    private static final UUID deletedPlanId = UUID.randomUUID();
    private static final User owner = getUser(UserStatus.ACTIVE, "owner");
    private static final User activeMember1 = getUser(UserStatus.ACTIVE, "활성화된 멤버 1");
    private static final User activeMember2 = getUser(UserStatus.ACTIVE, "활성화된 멤버 2");
    private static final User withdrawnMember = getUser(UserStatus.WITHDRAWN, "탈퇴한 멤버 1");
    private static final User deletedMember = getUser(UserStatus.DELETED, "삭제된 멤버 1");
    private static final User suspendedMember = getUser(UserStatus.SUSPENDED, "정지된 멤버 1");
    private static final User blockedMember = getUser(UserStatus.BLOCKED, "차단된 멤버 1");
    private static final User planWithdrawnMember = getUser(UserStatus.ACTIVE, "플랜에서 탈퇴한 멤버 1");
    private static final User planKickedMember = getUser(UserStatus.ACTIVE, "플랜에서 퇴출당한 멤버 1");
    private static final User planBlockedMember = getUser(UserStatus.ACTIVE, "플랜에서 차단당한 멤버 1");

    @BeforeEach
    void setUp() {
        userRepository.saveAll(List.of(
                owner, activeMember1, activeMember2, withdrawnMember, deletedMember,
                suspendedMember, blockedMember, planWithdrawnMember, planKickedMember, planBlockedMember
        ));

        savePlanWithUsers(
                activePlanId, "경주 여행", true, false,
                Map.of(
                        activeMember1, PlanUserStatus.MEMBER,
                        activeMember2, PlanUserStatus.MEMBER,
                        withdrawnMember, PlanUserStatus.MEMBER,
                        deletedMember, PlanUserStatus.MEMBER,
                        suspendedMember, PlanUserStatus.MEMBER,
                        blockedMember, PlanUserStatus.MEMBER,
                        planKickedMember, PlanUserStatus.KICKED,
                        planBlockedMember, PlanUserStatus.BLOCKED,
                        planWithdrawnMember, PlanUserStatus.WITHDRAWN,
                        owner, PlanUserStatus.OWNER
                )
        );

        savePlanWithUsers(
                privatePlanId, "경주 여행", false, false,
                Map.of(
                        activeMember1, PlanUserStatus.MEMBER,
                        activeMember2, PlanUserStatus.MEMBER,
                        withdrawnMember, PlanUserStatus.MEMBER,
                        deletedMember, PlanUserStatus.MEMBER,
                        suspendedMember, PlanUserStatus.MEMBER,
                        blockedMember, PlanUserStatus.MEMBER,
                        planKickedMember, PlanUserStatus.KICKED,
                        planBlockedMember, PlanUserStatus.BLOCKED,
                        planWithdrawnMember, PlanUserStatus.WITHDRAWN,
                        owner, PlanUserStatus.OWNER
                )
        );

        savePlanWithUsers(
                deletedPlanId, "삭제된 경주 여행", true, true,
                Map.of(
                        activeMember1, PlanUserStatus.MEMBER,
                        withdrawnMember, PlanUserStatus.MEMBER,
                        owner, PlanUserStatus.OWNER
                )
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("본인이 OWNER인 활성화된 여행 플랜을 성공적으로 조회한다.")
    @Test
    void shouldRetrieveActiveTravelPlan_whenUserIsOwner() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(activePlanId.toString()))
                .andExpect(jsonPath("$.name").value("경주 여행"))
                .andExpect(jsonPath("$.isPublic").value("true"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.ownerNickname").value(owner.getNickname()))
                .andExpect(jsonPath("$.startDate").value(LocalDate.parse("2025-09-29").toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.parse("2025-09-30").toString()))
                .andExpect(jsonPath("$.isOwner").value(true))
                .andExpect(jsonPath("$.isMember").value(false))
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        JsonNode memberArray = json.get("member");
        List<Tuple> actualMembers = new ArrayList<>();
        for (JsonNode node : memberArray) {
            actualMembers.add(
                    tuple(node.get("memberId").asText(), node.get("nickname").asText())
            );
        }

        assertThat(actualMembers).hasSize(2)
                .containsExactlyInAnyOrder(
                        tuple(activeMember1.getId().toString(), activeMember1.getNickname()),
                        tuple(activeMember2.getId().toString(), activeMember2.getNickname())
                );
    }

    @DisplayName("본인이 MEMBER인 활성화된 여행 플랜을 성공적으로 조회한다.")
    @Test
    void shouldRetrieveActiveTravelPlan_whenUserIsMember() throws Exception {
        //given
        String token = buildToken(activeMember1.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(activePlanId.toString()))
                .andExpect(jsonPath("$.name").value("경주 여행"))
                .andExpect(jsonPath("$.isPublic").value("true"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.ownerNickname").value(owner.getNickname()))
                .andExpect(jsonPath("$.startDate").value(LocalDate.parse("2025-09-29").toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.parse("2025-09-30").toString()))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.isMember").value(true))
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        JsonNode memberArray = json.get("member");
        List<Tuple> actualMembers = new ArrayList<>();
        for (JsonNode node : memberArray) {
            actualMembers.add(
                    tuple(node.get("memberId").asText(), node.get("nickname").asText())
            );
        }

        assertThat(actualMembers).hasSize(2)
                .containsExactlyInAnyOrder(
                        tuple(activeMember1.getId().toString(), activeMember1.getNickname()),
                        tuple(activeMember2.getId().toString(), activeMember2.getNickname())
                );
    }

    @DisplayName("본인이 OWNER인 삭제된 여행 플랜을 조회하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenOwnerRequestsDeletedPlan() throws Exception {
        //given
        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("본인이 MEMBER인 삭제된 여행 플랜을 조회하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenMemberRequestsDeletedPlan() throws Exception {
        //given
        String token = buildToken(activeMember1.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("이미 탈퇴한 멤버가 활성화된 플랜을 조회 할 경우 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(withdrawnMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 차단당한 멤버가 활성화된 플랜을 조회 할 경우 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(blockedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 삭제된 멤버가 활성화된 플랜을 조회 할 경우 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(deletedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 정지된 멤버가 활성화된 플랜을 조회 할 경우 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(suspendedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 탈퇴한 멤버가 본인이 MEMBER인 삭제된 여행 플랜을 조회하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnMemberRequestsDeletedPlan() throws Exception {
        //given
        String token = buildToken(withdrawnMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + deletedPlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("플랜에서 탈퇴한 멤버가 PRIVATE인 여행 플랜을 조회하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenWithdrawnFromPlanMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(planWithdrawnMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + privatePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("플랜에서 퇴출당한 멤버가 PRIVATE인 여행 플랜을 조회하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenDeletedFromPlanMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(planKickedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + privatePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("플랜에서 블락당한 멤버가 PRIVATE인 여행 플랜을 조회하면 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedFromPlanMemberRequestsPlan() throws Exception {
        //given
        String token = buildToken(planBlockedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + privatePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("플랜에서 탈퇴한 멤버가 PUBLIC인 여행 플랜을 성공적으로 조회한다.")
    @Test
    void shouldRetrievePublicPlanSuccessfully_whenWithdrawnMemberRequests() throws Exception {
        //given
        String token = buildToken(planWithdrawnMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(activePlanId.toString()))
                .andExpect(jsonPath("$.name").value("경주 여행"))
                .andExpect(jsonPath("$.isPublic").value("true"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.ownerNickname").value(owner.getNickname()))
                .andExpect(jsonPath("$.startDate").value(LocalDate.parse("2025-09-29").toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.parse("2025-09-30").toString()))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.isMember").value(false))
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        JsonNode memberArray = json.get("member");
        List<Tuple> actualMembers = new ArrayList<>();
        for (JsonNode node : memberArray) {
            actualMembers.add(
                    tuple(node.get("memberId").asText(), node.get("nickname").asText())
            );
        }

        assertThat(actualMembers).hasSize(2)
                .containsExactlyInAnyOrder(
                        tuple(activeMember1.getId().toString(), activeMember1.getNickname()),
                        tuple(activeMember2.getId().toString(), activeMember2.getNickname())
                );
    }

    @DisplayName("플랜에서 퇴출당한 멤버가 PUBLIC인 여행 플랜을 성공적으로 조회한다.")
    @Test
    void shouldRetrievePublicPlanSuccessfully_whenKickedMemberRequests() throws Exception {
        //given
        String token = buildToken(planKickedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(activePlanId.toString()))
                .andExpect(jsonPath("$.name").value("경주 여행"))
                .andExpect(jsonPath("$.isPublic").value("true"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.ownerNickname").value(owner.getNickname()))
                .andExpect(jsonPath("$.startDate").value(LocalDate.parse("2025-09-29").toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.parse("2025-09-30").toString()))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.isMember").value(false))
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        JsonNode memberArray = json.get("member");
        List<Tuple> actualMembers = new ArrayList<>();
        for (JsonNode node : memberArray) {
            actualMembers.add(
                    tuple(node.get("memberId").asText(), node.get("nickname").asText())
            );
        }

        assertThat(actualMembers).hasSize(2)
                .containsExactlyInAnyOrder(
                        tuple(activeMember1.getId().toString(), activeMember1.getNickname()),
                        tuple(activeMember2.getId().toString(), activeMember2.getNickname())
                );
    }

    @DisplayName("플랜에서 블락당한 멤버가 PUBLIC인 여행 플랜을 조회 할 경우 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedMemberRequestsPublicPlan() throws Exception {
        //given
        String token = buildToken(planBlockedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("존재하지 않는 플랜 ID로 조회하면 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanIdDoesNotExist() throws Exception {
        //given
        String token = buildToken(planBlockedMember.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("만료된 토큰으로 플랜을 조회하면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpired() throws Exception {
        //given
        String expiredToken = buildToken(owner.getId(), "access", issuer, Instant.now().minusSeconds(10));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("플랜에 속하지 않은 ACTIVE 유저가 PUBLIC 플랜 조회에 성공한다.")
    @Test
    void shouldReturn403_whenUnrelatedUserRequestsPublicPlan() throws Exception {
        //given
        User unrelatedUser = userRepository.save(getUser(UserStatus.ACTIVE, "외부인"));
        String token = buildToken(unrelatedUser.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + activePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(activePlanId.toString()))
                .andExpect(jsonPath("$.name").value("경주 여행"))
                .andExpect(jsonPath("$.isPublic").value("true"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.ownerNickname").value(owner.getNickname()))
                .andExpect(jsonPath("$.isOwner").value(false))
                .andExpect(jsonPath("$.isMember").value(false))
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        JsonNode memberArray = json.get("member");
        List<Tuple> actualMembers = new ArrayList<>();
        for (JsonNode node : memberArray) {
            actualMembers.add(
                    tuple(node.get("memberId").asText(), node.get("nickname").asText())
            );
        }

        assertThat(actualMembers).hasSize(2)
                .containsExactlyInAnyOrder(
                        tuple(activeMember1.getId().toString(), activeMember1.getNickname()),
                        tuple(activeMember2.getId().toString(), activeMember2.getNickname())
                );
    }

    @DisplayName("플랜에 속하지 않은 ACTIVE 유저가 PRIVATE 플랜 조회 시 403을 반환한다.")
    @Test
    void shouldReturn403_whenUnrelatedUserRequestsPrivatePlan() throws Exception {
        //given
        User unrelatedUser = userRepository.save(getUser(UserStatus.ACTIVE, "외부인"));
        String token = buildToken(unrelatedUser.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + privatePlanId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    private static User getUser(UserStatus status, String nickname) {
        UUID id = UUID.randomUUID();
        return User.builder()
                .id(id)
                .userId("member" + id)
                .password("passworddd")
                .status(status)
                .nickname(nickname)
                .build();
    }

    private void savePlanWithUsers(UUID planId, String name, boolean isPublic, boolean isDeleted,
                                   Map<User, PlanUserStatus> userStatusMap) {
        planRepository.save(
                Plan.builder()
                        .id(planId)
                        .name(name)
                        .isPublic(isPublic)
                        .isDeleted(isDeleted)
                        .startDate(LocalDate.parse("2025-09-29"))
                        .endDate(LocalDate.parse("2025-09-30"))
                        .build()
        );

        userStatusMap.forEach((user, status) -> savePlanUser(planId, user.getId(), status));
    }

    private void savePlanUser(UUID planId, UUID userId, PlanUserStatus status) {
        planUsersRepository.save(
                PlanUsers.builder()
                        .planId(planId)
                        .userId(userId)
                        .planUserStatus(status)
                        .build()
        );
    }

}
