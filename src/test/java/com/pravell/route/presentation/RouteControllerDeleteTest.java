package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.domain.model.Route;
import com.pravell.route.presentation.request.DeleteRouteRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RouteControllerDeleteTest extends RouteControllerTestSupport {

    @DisplayName("루트가 속한 플랜의 멤버, 소유자만 루트 삭제가 가능하다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideDeletableRoles")
    void shouldAllowRouteDeletion_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isTrue();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isTrue();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    private static Stream<Arguments> provideDeletableRoles() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("루트가 속한 플랜에서 탈퇴, 강퇴, 차단 비참여 유저는 루트 삭제가 불가능하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUndeletableRoles")
    void shouldReturn403_whenUserIsNotAllowedToDeleteRoute(String role, PlanUserStatus planUserStatus)
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

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 루트를 삭제 할 권한이 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    private static Stream<Arguments> provideUndeletableRoles() {
        return Stream.of(
                Arguments.of("탈퇴당한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("삭제 할 루트가 하나라도 존재하지 않으면 모든 루트 삭제가 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAnyRouteDoesNotExistDuringBatchDeletion() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId(), UUID.randomUUID()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    @DisplayName("삭제 할 루트가 하나라도 이미 삭제되었으면 모든 루트 삭제가 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAnyRouteIsAlreadyDeletedDuringBatchDeletion() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), true);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isTrue();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isTrue();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    @DisplayName("유저가 존재하지 않으면 루트 삭제가 불가능하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단된 유저는 루트 삭제가 불가능하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInvalidUsers")
    void shouldReturn404_whenUserIsInvalidOrInactive(String role, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    private static Stream<Arguments> provideInvalidUsers() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("삭제할 루트가 속한 플랜이 존재하지 않으면 루트 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanOfRouteDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        PlanUsers planUsers = getPlanUsers(UUID.randomUUID(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(UUID.randomUUID(), false);
        Route route2 = getRoute(UUID.randomUUID(), false);
        Route route3 = getRoute(UUID.randomUUID(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    @DisplayName("삭제할 루트가 속한 플랜이 삭제되었으면 루트 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsInvalidOdrInactive() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

    @DisplayName("토큰이 만료되었으면 루트 삭제에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route1 = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        Route route3 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route1, route2, route3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        DeleteRouteRequest request = DeleteRouteRequest.builder()
                .routeId(List.of(route1.getId(), route2.getId()))
                .build();

        assertThat(route1.isDeleted()).isFalse();
        assertThat(route2.isDeleted()).isFalse();
        assertThat(route3.isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Route> afterRoute1 = routeRepository.findById(route1.getId());
        assertThat(afterRoute1).isPresent();
        assertThat(afterRoute1.get().isDeleted()).isFalse();
        Optional<Route> afterRoute2 = routeRepository.findById(route2.getId());
        assertThat(afterRoute2).isPresent();
        assertThat(afterRoute2.get().isDeleted()).isFalse();
        Optional<Route> afterRoute3 = routeRepository.findById(route3.getId());
        assertThat(afterRoute3).isPresent();
        assertThat(afterRoute3.get().isDeleted()).isFalse();
    }

}
