package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
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
        User user1 = getUser("유저 1", UserStatus.ACTIVE);
        User user2 = getUser("유저 2", UserStatus.ACTIVE);
        User user3 = getUser("유저 3", UserStatus.BLOCKED);
        User user4 = getUser("유저 4", UserStatus.ACTIVE);
        User user5 = getUser("유저 5", UserStatus.WITHDRAWN);
        userRepository.saveAll(List.of(user1, user2, user3, user4, user5));

        Plan plan1 = getPlan("정상 조회 플랜1", true, false, LocalDate.parse("2025-09-20"), LocalDate.parse("2025-09-29"));
        PlanUsers planUsers1 = getPlanUsers(PlanUserStatus.OWNER, plan1.getId(), user1.getId());
        PlanUsers planUsers2 = getPlanUsers(PlanUserStatus.MEMBER, plan1.getId(), user2.getId());
        PlanUsers planUsers3 = getPlanUsers(PlanUserStatus.MEMBER, plan1.getId(), user3.getId());

        Plan plan2 = getPlan("정상 조회 플랜2", false, false, LocalDate.parse("2025-10-10"), LocalDate.parse("2025-11-10"));
        PlanUsers planUsers4 = getPlanUsers(PlanUserStatus.OWNER, plan2.getId(), user2.getId());
        PlanUsers planUsers5 = getPlanUsers(PlanUserStatus.MEMBER, plan2.getId(), user4.getId());
        PlanUsers planUsers6 = getPlanUsers(PlanUserStatus.MEMBER, plan2.getId(), user5.getId());
        PlanUsers planUsers14 = getPlanUsers(PlanUserStatus.MEMBER, plan2.getId(), user1.getId());

        Plan plan3 = getPlan("정상 조회 플랜3", true, false, LocalDate.parse("2025-12-20"), LocalDate.parse("2025-12-29"));
        PlanUsers planUsers7 = getPlanUsers(PlanUserStatus.OWNER, plan3.getId(), user2.getId());
        PlanUsers planUsers8 = getPlanUsers(PlanUserStatus.MEMBER, plan3.getId(), user1.getId());
        PlanUsers planUsers9 = getPlanUsers(PlanUserStatus.MEMBER, plan3.getId(), user5.getId());

        Plan plan4 = getPlan("유저가 속하지 않은 플랜1", true, false, LocalDate.parse("2025-12-20"),
                LocalDate.parse("2025-12-29"));
        PlanUsers planUsers15 = getPlanUsers(PlanUserStatus.OWNER, plan4.getId(), user2.getId());
        PlanUsers planUsers16 = getPlanUsers(PlanUserStatus.MEMBER, plan4.getId(), user5.getId());
        Plan plan5 = getPlan("삭제된 플랜1", true, true, LocalDate.parse("2025-12-20"), LocalDate.parse("2025-12-29"));
        PlanUsers planUsers10 = getPlanUsers(PlanUserStatus.MEMBER, plan5.getId(), user1.getId());
        Plan plan6 = getPlan("탈퇴한 플랜1", true, false);
        PlanUsers planUsers11 = getPlanUsers(PlanUserStatus.WITHDRAWN, plan6.getId(), user1.getId());
        Plan plan7 = getPlan("강퇴당한 플랜1", true, false);
        PlanUsers planUsers12 = getPlanUsers(PlanUserStatus.KICKED, plan7.getId(), user1.getId());
        Plan plan8 = getPlan("차단당한 플랜1", true, false);
        PlanUsers planUsers13 = getPlanUsers(PlanUserStatus.KICKED, plan8.getId(), user1.getId());

        planRepository.saveAll(List.of(plan1, plan2, plan3, plan4, plan5, plan6, plan7, plan8));
        planUsersRepository.saveAll(
                List.of(planUsers1, planUsers2, planUsers3, planUsers4, planUsers5, planUsers6, planUsers7, planUsers8,
                        planUsers9, planUsers10, planUsers11, planUsers12, planUsers13, planUsers14, planUsers15,
                        planUsers16));

        String token = buildToken(user1.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andDo(result -> {
                    List<Map<String, Object>> response = objectMapper.readValue(
                            result.getResponse().getContentAsString(),
                            new TypeReference<>() {}
                    );

                    List<Tuple> actual = response.stream()
                            .map(map -> tuple(
                                    UUID.fromString((String) map.get("planId")),
                                    map.get("planName"),
                                    map.get("isOwner"),
                                    LocalDate.parse((String) map.get("startDate")),
                                    LocalDate.parse((String) map.get("endDate")),
                                    ((List<?>) map.get("members")).stream()
                                            .map(Object::toString)
                                            .sorted()
                                            .collect(Collectors.toList())
                            ))
                            .collect(Collectors.toList());

                    assertThat(actual).containsExactlyInAnyOrder(
                            tuple(
                                    plan1.getId(),
                                    plan1.getName(),
                                    true,
                                    plan1.getStartDate(),
                                    plan1.getEndDate(),
                                    Stream.of(user1.getNickname(), user2.getNickname()).sorted().toList()
                            ),
                            tuple(
                                    plan2.getId(),
                                    plan2.getName(),
                                    false,
                                    plan2.getStartDate(),
                                    plan2.getEndDate(),
                                    Stream.of(user1.getNickname(), user2.getNickname(), user4.getNickname()).sorted().toList()
                            ),
                            tuple(
                                    plan3.getId(),
                                    plan3.getName(),
                                    false,
                                    plan3.getStartDate(),
                                    plan3.getEndDate(),
                                    Stream.of(user1.getNickname(), user2.getNickname()).sorted().toList()
                            )
                    );
                });
    }

    @DisplayName("로그인 한 유저가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenLoggedInUserNotFound() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "111")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    private Plan getPlan(String name, boolean isPublic, boolean isDeleted) {
        return getPlan(name, isPublic, isDeleted, LocalDate.parse("2025-09-29"), LocalDate.parse("2025-09-30"));
    }

    private Plan getPlan(String name, boolean isPublic, boolean isDeleted, LocalDate startDate, LocalDate endDate) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isPublic(isPublic)
                .isDeleted(isDeleted)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private PlanUsers getPlanUsers(PlanUserStatus status, UUID planId, UUID userId) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

    private static User getUser(String nickname, UserStatus userStatus) {
        return User.builder()
                .id(UUID.randomUUID())
                .userId("userId" + UUID.randomUUID().toString())
                .password("passwordd")
                .nickname(nickname)
                .status(userStatus)
                .build();
    }

}