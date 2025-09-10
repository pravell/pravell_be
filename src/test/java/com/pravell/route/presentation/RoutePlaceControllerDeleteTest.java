package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.presentation.request.DeleteRoutePlacesRequest;
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

class RoutePlaceControllerDeleteTest extends RoutePlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        routePlaceRepository.deleteAllInBatch();
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜의 멤버, 소유자라면 루트에 저장된 장소를 삭제할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedRolesForRoutePlaceDeletion")
    void shouldDeleteRoutePlace_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId()))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(1L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isNotPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isNotPresent();
    }

    private static Stream<Arguments> provideAuthorizedRolesForRoutePlaceDeletion() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜에서 탈퇴, 강퇴, 차단당했거나 비참여 유저는 루트에 저장된 장소를 삭제할 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedRolesForRoutePlaceDeletion")
    void shouldFailToDeleteRoutePlace_whenUserIsNotParticipant(String role, PlanUserStatus planUserStatus)
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

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId()))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 삭제 할 권한이 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    private static Stream<Arguments> provideUnauthorizedRolesForRoutePlaceDeletion() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("삭제하려는 장소가 하나라도 존재하지 않는다면 모든 장소를 삭제할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlaces_whenAnyRoutePlaceDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId(), 1000L))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트에서 해당 장소를 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("장소가 저장된 루트가 존재하지 않는다면 모든 장소를 삭제할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlaces_whenRouteDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId(), 1000L))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + UUID.randomUUID() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("장소가 저장된 루트가 삭제되었다면 모든 장소를 삭제할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlaces_whenRouteIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), true);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId(), 1000L))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜이 삭제되었다면 모든 장소를 삭제할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlaces_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId(), 1000L))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜이 존재하지 않으면 모든 장소를 삭제할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlaces_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Route route = getRoute(UUID.randomUUID(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId(), 1000L))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단당한 유저는 루트에 저장된 장소를 삭제할 수 없고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatusesForRoutePlaceDeletion")
    void shouldFailToDeleteRoutePlace_whenUserIsWithdrawnOrBanned(String state, UserStatus userStatus)
            throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId()))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    private static Stream<Arguments> provideInactiveUserStatusesForRoutePlaceDeletion() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("유저가 존재하지 않으면 루트에 저장된 장소를 삭제할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlace_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId()))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("토큰이 만료되었으면 루트에 저장된 장소를 삭제할 수 없고, 401을 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlace_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId()))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

    @DisplayName("토큰이 변조되었으면 루트에 저장된 장소를 삭제할 수 없고, 401을 반환한다.")
    @Test
    void shouldFailToDeleteRoutePlace_whenTokenIsTampered() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), 10L);
        RoutePlace routePlace2 = getRoutePlace(route.getId(), 13L);
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 14L);
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000)) + "aa";

        DeleteRoutePlacesRequest request = DeleteRoutePlacesRequest.builder()
                .deleteRoutePlaceId(List.of(routePlace.getId(), routePlace2.getId()))
                .build();

        assertThat(routePlaceRepository.count()).isEqualTo(3L);

        //when
        mockMvc.perform(
                        delete("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(routePlaceRepository.count()).isEqualTo(3L);
        assertThat(routePlaceRepository.findById(routePlace.getId())).isPresent();
        assertThat(routePlaceRepository.findById(routePlace2.getId())).isPresent();
    }

}
