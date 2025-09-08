package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.domain.model.Route;
import com.pravell.route.presentation.request.CreateRouteRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
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

class RouteControllerCreateTest extends RouteControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
    }

    @DisplayName("루트를 생성할 플랜의 멤버, 소유자면 루트를 생성할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideValidRouteCreationAuthorities")
    void shouldCreateRoute_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeId").isNotEmpty())
                .andExpect(jsonPath("$.planId").value(request.getPlanId().toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(contentAsString, new TypeReference<>() {
        });
        UUID routeId = UUID.fromString(responseMap.get("routeId").toString());

        //then
        Optional<Route> after = routeRepository.findById(routeId);
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(request.getName());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getPlanId()).isEqualTo(request.getPlanId());
    }

    private static Stream<Arguments> provideValidRouteCreationAuthorities() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("루트 설명이 없어도 루트 생성에 성공한다.")
    @Test
    void shouldCreatePlanSuccessfully_whenDescriptionIsMissing() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .build();

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routeId").isNotEmpty())
                .andExpect(jsonPath("$.planId").value(request.getPlanId().toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(contentAsString, new TypeReference<>() {
        });
        UUID routeId = UUID.fromString(responseMap.get("routeId").toString());

        //then
        Optional<Route> after = routeRepository.findById(routeId);
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(request.getName());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getPlanId()).isEqualTo(request.getPlanId());
    }

    @DisplayName("루트를 생성할 플랜에서 탈퇴, 강퇴, 차단, 비참여 유저는 루트를 생성할 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideInvalidRouteCreationAuthorities")
    void shouldNotCreateRoute_whenUserHasNoCreationAuthority(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 루트를 생성 할 권한이 없습니다."));

        assertThat(routeRepository.count()).isZero();
    }

    private static Stream<Arguments> provideInvalidRouteCreationAuthorities() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("루트를 생성할 플랜이 존재하지 않으면 루트 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenCreatingRouteWithNonExistentPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(UUID.randomUUID())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("루트 이름이 2자 미만이면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 이름은 2 ~ 30자 사이여야 합니다."));

        assertThat(routeRepository.count()).isZero();
    }


    @DisplayName("루트 이름이 30자 초과면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루".repeat(31))
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 이름은 2 ~ 30자 사이여야 합니다."));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("루트 이름이 null이면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsNull() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 이름은 생략이 불가능합니다."));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("루트 이름이 공백이면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsBlank() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name(" ")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("name: 이름은 생략이 불가능합니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("루트 설명이 2자 미만이면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("description: 루트 설명은 2 ~ 50자 사이여야 합니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("루트 설명이 50자 초과면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루".repeat(51))
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("description: 루트 설명은 2 ~ 50자 사이여야 합니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("루트를 저장할 플랜을 지정하지 않으면 루트 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenPlanIdIsNull() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("planId: 플랜을 지정해야 합니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("유저가 존재하지 않으면 루트 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("유저를 찾을 수 없습니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단된 유저는 루트 생성에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveOrBlockedUsers")
    void shouldFailToCreateRoute_whenUserIsDeletedOrBlocked(String role, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("유저를 찾을 수 없습니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    private static Stream<Arguments> provideInactiveOrBlockedUsers() {
        return Stream.of(
                Arguments.of("탈퇴", UserStatus.WITHDRAWN),
                Arguments.of("삭제", UserStatus.DELETED),
                Arguments.of("정지", UserStatus.SUSPENDED),
                Arguments.of("차단", UserStatus.BLOCKED)
        );
    }

    @DisplayName("액세스 토큰이 만료되었으면 루트 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToCreateRoute_whenAccessTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message", containsString("토큰이 올바르지 않습니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("액세스 토큰이 변조되었으면 루트 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsTampered() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000))+"aa";

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message", containsString("토큰이 올바르지 않습니다.")));

        assertThat(routeRepository.count()).isZero();
    }

    @DisplayName("액세스 토큰이 아니라면 루트 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsNotAccessToken() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().plusSeconds(100000));

        CreateRouteRequest request = CreateRouteRequest.builder()
                .planId(plan.getId())
                .name("루트 이름")
                .description("루트 설명")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message", containsString("토큰이 올바르지 않습니다.")));

        assertThat(routeRepository.count()).isZero();
    }

}
