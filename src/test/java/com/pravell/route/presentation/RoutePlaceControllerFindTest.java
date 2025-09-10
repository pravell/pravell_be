package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.application.dto.response.RoutePlaceResponse;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
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

class RoutePlaceControllerFindTest extends RoutePlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
    }

    @DisplayName("루트가 속한 플랜이 PRIVATE 이면 멤버, 소유자만 루트에 저장된 장소들을 조회할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideRolesForPrivatePlanAccess")
    void shouldAllowRoutePlaceView_whenPrivatePlanAndUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route, route2));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));

        RoutePlace routePlace4 = getRoutePlace(route2.getId(), pinPlace3.getId(), 3L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace5 = getRoutePlace(route2.getId(), pinPlace3.getId(), 4L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3, routePlace4, routePlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        List<RoutePlaceResponse> result = objectMapper.readValue(contentAsString,
                new TypeReference<List<RoutePlaceResponse>>() {
                });

        assertThat(result).hasSize(3)
                .extracting("pinPlaceId", "title", "nickname", "description", "sequence", "date", "address",
                        "isPinPlaceDeleted")
                .containsExactlyInAnyOrder(
                        tuple(routePlace.getPinPlaceId(), pinPlace.getTitle(), routePlace.getNickname(),
                                routePlace.getDescription(), routePlace.getSequence(), routePlace.getDate(),
                                pinPlace.getAddress(), false),
                        tuple(routePlace2.getPinPlaceId(), pinPlace2.getTitle(), routePlace2.getNickname(),
                                routePlace2.getDescription(), routePlace2.getSequence(), routePlace2.getDate(),
                                pinPlace2.getAddress(), false),
                        tuple(routePlace3.getPinPlaceId(), null, routePlace3.getNickname(),
                                routePlace3.getDescription(), routePlace3.getSequence(), routePlace3.getDate(),
                                null, true)
                );
    }

    private static Stream<Arguments> provideRolesForPrivatePlanAccess() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("루트가 속한 플랜이 PUBLIC 이면 멤버, 소유자, 탈퇴, 강퇴, 비참여 유저가 루트에 저장된 장소들을 조회할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideRolesForPublicPlanAccess")
    void shouldAllowRoutePlaceView_whenPublicPlanAndUserIsNotRestricted(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route, route2));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));

        RoutePlace routePlace4 = getRoutePlace(route2.getId(), pinPlace3.getId(), 3L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace5 = getRoutePlace(route2.getId(), pinPlace3.getId(), 4L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3, routePlace4, routePlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        List<RoutePlaceResponse> result = objectMapper.readValue(contentAsString,
                new TypeReference<List<RoutePlaceResponse>>() {
                });

        assertThat(result).hasSize(3)
                .extracting("pinPlaceId", "title", "nickname", "description", "sequence", "date", "address",
                        "isPinPlaceDeleted")
                .containsExactlyInAnyOrder(
                        tuple(routePlace.getPinPlaceId(), pinPlace.getTitle(), routePlace.getNickname(),
                                routePlace.getDescription(), routePlace.getSequence(), routePlace.getDate(),
                                pinPlace.getAddress(), false),
                        tuple(routePlace2.getPinPlaceId(), pinPlace2.getTitle(), routePlace2.getNickname(),
                                routePlace2.getDescription(), routePlace2.getSequence(), routePlace2.getDate(),
                                pinPlace2.getAddress(), false),
                        tuple(routePlace3.getPinPlaceId(), null, routePlace3.getNickname(),
                                routePlace3.getDescription(), routePlace3.getSequence(), routePlace3.getDate(),
                                null, true)
                );
    }

    private static Stream<Arguments> provideRolesForPublicPlanAccess() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER),
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("루트가 속한 플랜이 PRIVATE 이면 탈퇴, 강퇴, 차단, 비참여 유저는 루트에 저장된 장소들을 조회할 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideRestrictedRolesForPrivatePlan")
    void shouldDenyRoutePlaceView_whenPrivatePlanAndUserIsNotAuthorized(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route, route2));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));

        RoutePlace routePlace4 = getRoutePlace(route2.getId(), pinPlace3.getId(), 3L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace5 = getRoutePlace(route2.getId(), pinPlace3.getId(), 4L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3, routePlace4, routePlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 조회 할 권한이 없습니다."));
    }

    private static Stream<Arguments> provideRestrictedRolesForPrivatePlan() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단된 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("루트가 속한 플랜이 PUBLIC 이면 차단당한 유저는 루트에 저장된 장소들을 조회할 수 없고, 403을 반환한다.")
    @Test
    void shouldDenyRoutePlaceView_whenPublicPlanAndUserIsBanned() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        Route route2 = getRoute(plan.getId(), false);
        routeRepository.saveAll(List.of(route, route2));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));

        RoutePlace routePlace4 = getRoutePlace(route2.getId(), pinPlace3.getId(), 3L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace5 = getRoutePlace(route2.getId(), pinPlace3.getId(), 4L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3, routePlace4, routePlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 조회 할 권한이 없습니다."));
    }

    @DisplayName("루트가 속한 플랜이 존재하지 않으면 루트에 저장된 장소들을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Route route = getRoute(UUID.randomUUID(), false);
        routeRepository.saveAll(List.of(route));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("루트가 속한 플랜이 삭제되었으면 루트에 저장된 장소들을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("루트가 삭제되었으면 루트에 저장된 장소들을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenRouteIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), true);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));
    }

    @DisplayName("루트가 존재하지 않으면 루트에 저장된 장소들을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenRouteDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + UUID.randomUUID() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));
    }

    @DisplayName("유저가 탈퇴, 삭제, 정지, 차단되었으면 루트에 저장된 장소들을 조회할 수 없고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatuses")
    void shouldFailToViewRoutePlaces_whenUserIsInactiveOrBlocked(String status, UserStatus userStatus)
            throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideInactiveUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("유저가 존재하지 않으면 루트에 저장된 장소들을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("토큰이 만료되었으면 루트에 저장된 장소들을 조회할 수 없고, 401을 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("토큰이 변조되었으면 루트에 저장된 장소들을 조회할 수 없고, 401을 반환한다.")
    @Test
    void shouldFailToViewRoutePlaces_whenTokenIsTampered() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace, pinPlace2, pinPlace3));

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L,
                "11시 퇴실", "숙소", LocalDate.parse("2025-09-30"));
        RoutePlace routePlace2 = getRoutePlace(route.getId(), pinPlace2.getId(), 2L,
                "null", "null", LocalDate.parse("2025-09-29"));
        RoutePlace routePlace3 = getRoutePlace(route.getId(), 1000L, 3L,
                "null", "카페", LocalDate.parse("2025-09-30"));
        routePlaceRepository.saveAll(List.of(routePlace, routePlace2, routePlace3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000)) + "aa";

        //when, then
        mockMvc.perform(
                        get("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

}
