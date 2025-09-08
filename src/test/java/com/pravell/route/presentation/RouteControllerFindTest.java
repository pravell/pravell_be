package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.application.dto.response.FindRoutesResponse;
import com.pravell.route.domain.model.Route;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class RouteControllerFindTest extends RouteControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
    }

    @DisplayName("PRIVATE 플랜은 플랜의 멤버, 소유자만 플랜에 속한 루트 조회가 가능하다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("providePrivatePlanUserRoles")
    void shouldAllowRouteAccess_whenUserIsOwnerOrMemberInPrivatePlan(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String content = mvcResult.getResponse().getContentAsString();

        List<FindRoutesResponse> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        assertThat(responseList).hasSize(2)
                .extracting("routeId", "name", "description")
                .containsExactlyInAnyOrder(
                        tuple(route1.getId(), route1.getName(), route1.getDescription()),
                        tuple(route2.getId(), route2.getName(), route2.getDescription()));
    }

    private static Stream<Arguments> providePrivatePlanUserRoles() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("PRIVATE 플랜은 플랜에서 탈퇴, 강퇴, 차단 비참여 유저는 플랜에 속한 루트 조회 불가능하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideInvalidPrivatePlanAccessRoles")
    void shouldReturn403_whenUserIsNotParticipantOfPrivatePlan(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    private static Stream<Arguments> provideInvalidPrivatePlanAccessRoles() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("PUBLIC 플랜은 플랜의 멤버, 소유자, 탈퇴, 강퇴, 비참여 유저가 플랜에 속한 루트 조회가 가능하다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAllPublicPlanAccessRoles")
    void shouldAllowRouteAccess_whenUserHasAnyRoleInPublicPlan(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String content = mvcResult.getResponse().getContentAsString();

        List<FindRoutesResponse> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        assertThat(responseList).hasSize(2)
                .extracting("routeId", "name", "description")
                .containsExactlyInAnyOrder(
                        tuple(route1.getId(), route1.getName(), route1.getDescription()),
                        tuple(route2.getId(), route2.getName(), route2.getDescription()));
    }

    private static Stream<Arguments> provideAllPublicPlanAccessRoles() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER),
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("PUBLIC 플랜은 플랜에서 차단당한 유저는 플랜에 속한 루트 조회 불가능하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUserIsBlockedInPublicPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("삭제된 플랜은 플랜에 속한 루트 조회가 불가능하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingRoutesInDeletedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("플랜이 존재하지 않으면 플랜에 속한 루트 조회가 불가능하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingRoutesInNonExistentPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("유저가 존재하지 않으면 플랜에 속한 루트 조회가 불가능하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingRoutesInNonExistentUser() throws Exception {
        //given
        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단된 유저는 플랜에 속한 루트 조회가 불가능하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideDeactivatedUserStatuses")
    void shouldReturn404_whenAccessingRoutesAsDeactivatedUser(String statue, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideDeactivatedUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("토큰이 만료되었으면 플랜에 속한 루트 조회 불가능하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("토큰이 변조되었으면 플랜에 속한 루트 조회 불가능하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsTampered() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000)) + "aa";

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("access token이 아니면 플랜에 속한 루트 조회 불가능하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAuthorizationHeaderIsNotAccessToken() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId());
        Route route2 = getRoute(plan.getId());

        Route route3 = getRoute(plan2.getId());
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

}
