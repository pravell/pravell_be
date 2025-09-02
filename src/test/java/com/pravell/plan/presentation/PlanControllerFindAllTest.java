package com.pravell.plan.presentation;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("플랜 전체 조회 통합 테스트")
class PlanControllerFindAllTest extends ControllerTestSupport {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("유저가 참여중이고 삭제되지 않은 플랜들을 조회한다.")
    @Test
    void shouldRetrieveAllActivePlansUserParticipatesIn() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.ACTIVE)
                .build());

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

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].planId", containsInAnyOrder(
                        plan1.getId().toString(),
                        plan2.getId().toString(),
                        plan3.getId().toString())))
                .andExpect(jsonPath("$[*].planName", containsInAnyOrder(
                        plan1.getName(),
                        plan2.getName(),
                        plan3.getName())))
                .andExpect(jsonPath("$[*].isOwner", containsInAnyOrder(true, true, false)));
    }

    @DisplayName("로그인 한 유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenLoggedInUserNotFound() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("로그인 한 유저가 이미 탈퇴한 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenLoggedInUserIsWithdrawn() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.WITHDRAWN)
                .build());

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                get("/api/v1/plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("로그인 한 유저가 이미 삭제된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenLoggedInUserIsDeleted() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.DELETED)
                .build());

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("로그인 한 유저가 이미 정지된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenLoggedInUserIsSuspended() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.SUSPENDED)
                .build());

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("로그인 한 유저가 이미 차단된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenLoggedInUserIsBlocked() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build());

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("토큰이 만료되었으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build());

        String token = buildToken(userId, "access", issuer, Instant.now().minusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("토큰이 accessToken이 아니면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsNotAccessToken() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build());

        String token = buildToken(userId, "refresh", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("토큰이 변조되었으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsTampered() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        userRepository.save(User.builder()
                .id(userId)
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build());

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer "+token+"111")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    private Plan getPlan(String name, boolean isPublic, boolean isDeleted) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isPublic(isPublic)
                .isDeleted(isDeleted)
                .build();
    }

    private PlanUsers getPlanUsers(PlanUserStatus status, UUID planId, UUID userId) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

}