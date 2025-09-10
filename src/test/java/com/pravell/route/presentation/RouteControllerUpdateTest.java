package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.domain.model.Route;
import com.pravell.route.presentation.request.UpdateRouteRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
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

class RouteControllerUpdateTest extends RouteControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
    }

    @DisplayName("루트가 속한 플랜의 멤버, 소유자라면 루트 업데이트에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedRolesForUpdate")
    void shouldUpdateRouteSuccessfully_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(route.getId().toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(route.getName());
        assertThat(after.get().getName()).isEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(route.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideAuthorizedRolesForUpdate() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("루트 이름만 업데이트할 수 있다.")
    @Test
    void shouldUpdateOnlyNameSuccessfully() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(route.getId().toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(route.getDescription()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(route.getName());
        assertThat(after.get().getName()).isEqualTo(request.getName());
        assertThat(after.get().getDescription()).isEqualTo(route.getDescription());
    }

    @DisplayName("루트 설명만 업데이트할 수 있다.")
    @Test
    void shouldUpdateOnlyDescriptionSuccessfully() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(route.getId().toString()))
                .andExpect(jsonPath("$.name").value(route.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isEqualTo(route.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(route.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
    }

    @DisplayName("루트가 속한 플랜에서 탈퇴, 강퇴, 차단당했거나 비참여 유저는 루트 업데이트에 실패하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedRolesForUpdate")
    void shouldReturn403_whenUserIsNotAllowedToUpdateRoute(String role, PlanUserStatus planUserStatus)
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

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 루트를 수정 할 권한이 없습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideUnauthorizedRolesForUpdate() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("루트가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenRouteDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));
    }

    @DisplayName("루트가 삭제되었으면 루트 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenRouteIsAlreadyDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), true);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("변경 할 이름이 2자 미만이면 루트 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("이")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 이름은 2 ~ 30자 사이여야 합니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("변경 할 이름이 30자 초과면 루트 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNameIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("이".repeat(31))
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 이름은 2 ~ 30자 사이여야 합니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("변경 할 설명이 2자 미만이면 루트 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("설")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: 루트 설명은 2 ~ 50자 사이여야 합니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("변경 할 설명이 50자 초과면 루트 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("설".repeat(51))
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: 루트 설명은 2 ~ 50자 사이여야 합니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("루트가 속한 플랜이 삭제되었으면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanOfRouteIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("루트가 속한 플랜이 존재하지 않으면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoute_whenRoutePlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Route route = getRoute(UUID.randomUUID(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("유저가 존재하지 않으면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단된 유저는 루트 업데이트에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInvalidUserStatuses")
    void shouldReturn404_whenUserIsInactive(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideInvalidUserStatuses(){
        return Stream.of(
          Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.DELETED)
        );
    }

    @DisplayName("토큰이 만료되었으면 루트 업데이트에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(1000));

        UpdateRouteRequest request = UpdateRouteRequest.builder()
                .name("변경 할 이름")
                .description("변경 할 설명")
                .build();

        Optional<Route> before = routeRepository.findById(route.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getName()).isEqualTo(route.getName());
        assertThat(before.get().getName()).isNotEqualTo(request.getName());
        assertThat(before.get().getDescription()).isEqualTo(route.getDescription());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Route> after = routeRepository.findById(route.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getName()).isNotEqualTo(request.getName());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

}
