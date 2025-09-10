package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.presentation.request.SaveRoutePlaceRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
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

class RoutePlaceControllerSaveTest extends RoutePlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        routePlaceRepository.deleteAllInBatch();
    }

    @DisplayName("루트가 속한 플랜의 멤버, 소유자라면 루트에 장소를 저장할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedRolesForPlaceCreation")
    void shouldSavePlaceToRoute_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routePlaceId").isNotEmpty())
                .andExpect(jsonPath("$.pinPlaceId").value(request.getPinPlaceId()))
                .andExpect(jsonPath("$.title").value(pinPlace.getTitle()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.sequence").value(1L))
                .andExpect(jsonPath("$.date").value(request.getDate().toString()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.roadAddress").value(pinPlace.getRoadAddress()))
                .andExpect(jsonPath("$.mapx").value(pinPlace.getMapx()))
                .andExpect(jsonPath("$.mapy").value(pinPlace.getMapy()))
                .andExpect(jsonPath("$.lat").value(pinPlace.getLatitude()))
                .andExpect(jsonPath("$.lng").value(pinPlace.getLongitude()))
                .andExpect(jsonPath("$.color").value(pinPlace.getPinColor()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(contentAsString);
        Long routePlaceId = Long.parseLong(jsonNode.get("routePlaceId").asText());

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlaceId);
        assertThat(after).isPresent();
        assertThat(after.get().getRouteId()).isEqualTo(route.getId());
        assertThat(after.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getSequence()).isEqualTo(1L);
    }

    private static Stream<Arguments> provideAuthorizedRolesForPlaceCreation() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("닉네임을 지정하지 않아도 루트에 장소를 저장할 수 있다.")
    @Test
    void shouldSavePlaceToRoute_whenNicknameIsNotProvided() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routePlaceId").isNotEmpty())
                .andExpect(jsonPath("$.pinPlaceId").value(request.getPinPlaceId()))
                .andExpect(jsonPath("$.title").value(pinPlace.getTitle()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.sequence").value(1L))
                .andExpect(jsonPath("$.date").value(request.getDate().toString()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.roadAddress").value(pinPlace.getRoadAddress()))
                .andExpect(jsonPath("$.mapx").value(pinPlace.getMapx()))
                .andExpect(jsonPath("$.mapy").value(pinPlace.getMapy()))
                .andExpect(jsonPath("$.lat").value(pinPlace.getLatitude()))
                .andExpect(jsonPath("$.lng").value(pinPlace.getLongitude()))
                .andExpect(jsonPath("$.color").value(pinPlace.getPinColor()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(contentAsString);
        Long routePlaceId = Long.parseLong(jsonNode.get("routePlaceId").asText());

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlaceId);
        assertThat(after).isPresent();
        assertThat(after.get().getRouteId()).isEqualTo(route.getId());
        assertThat(after.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getSequence()).isEqualTo(1L);
    }

    @DisplayName("장소 설명을 지정하지 않아도 루트에 장소를 저장할 수 있다.")
    @Test
    void shouldSavePlaceToRoute_whenDescriptionIsNotProvided() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routePlaceId").isNotEmpty())
                .andExpect(jsonPath("$.pinPlaceId").value(request.getPinPlaceId()))
                .andExpect(jsonPath("$.title").value(pinPlace.getTitle()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.sequence").value(1L))
                .andExpect(jsonPath("$.date").value(request.getDate().toString()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.roadAddress").value(pinPlace.getRoadAddress()))
                .andExpect(jsonPath("$.mapx").value(pinPlace.getMapx()))
                .andExpect(jsonPath("$.mapy").value(pinPlace.getMapy()))
                .andExpect(jsonPath("$.lat").value(pinPlace.getLatitude()))
                .andExpect(jsonPath("$.lng").value(pinPlace.getLongitude()))
                .andExpect(jsonPath("$.color").value(pinPlace.getPinColor()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(contentAsString);
        Long routePlaceId = Long.parseLong(jsonNode.get("routePlaceId").asText());

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlaceId);
        assertThat(after).isPresent();
        assertThat(after.get().getRouteId()).isEqualTo(route.getId());
        assertThat(after.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getSequence()).isEqualTo(1L);
    }

    @DisplayName("기존에 저장되어있던 sequence 다음 값으로 sequence 값이 저장된다.")
    @Test
    void shouldSavePlaceToRouteWithNextSequenceValue() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        routePlaceRepository.save(RoutePlace.builder()
                .routeId(route.getId())
                .pinPlaceId(pinPlace.getId())
                .sequence(1L)
                .date(LocalDate.now())
                .build());

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.routePlaceId").isNotEmpty())
                .andExpect(jsonPath("$.pinPlaceId").value(request.getPinPlaceId()))
                .andExpect(jsonPath("$.title").value(pinPlace.getTitle()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.sequence").value(2L))
                .andExpect(jsonPath("$.date").value(request.getDate().toString()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.roadAddress").value(pinPlace.getRoadAddress()))
                .andExpect(jsonPath("$.mapx").value(pinPlace.getMapx()))
                .andExpect(jsonPath("$.mapy").value(pinPlace.getMapy()))
                .andExpect(jsonPath("$.lat").value(pinPlace.getLatitude()))
                .andExpect(jsonPath("$.lng").value(pinPlace.getLongitude()))
                .andExpect(jsonPath("$.color").value(pinPlace.getPinColor()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(contentAsString);
        Long routePlaceId = Long.parseLong(jsonNode.get("routePlaceId").asText());

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlaceId);
        assertThat(after).isPresent();
        assertThat(after.get().getRouteId()).isEqualTo(route.getId());
        assertThat(after.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getSequence()).isEqualTo(2L);
    }

    @DisplayName("루트가 속한 플랜에서 탈퇴, 강퇴, 차단당했거나 비참여 유저라면 루트에 장소를 저장할 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedRolesForPlaceCreation")
    void shouldFailToSavePlaceToRoute_whenUserIsNotParticipant(String role, PlanUserStatus planUserStatus)
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

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 저장 할 권한이 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    private static Stream<Arguments> provideUnauthorizedRolesForPlaceCreation() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("루트가 삭제되었으면 루트에 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenRouteIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), true);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("루트가 존재하지 않으면 루트에 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenRouteDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + UUID.randomUUID() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("저장된 장소가 존재하지 않으면 루트에 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenPinPlaceDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(1000L)
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("저장된 장소를 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("루트가 속한 플랜이 삭제되었으면 루트에 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("루트가 속한 플랜이 존재하지 않으면 루트에 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        PinPlace pinPlace = getPinPlace(UUID.randomUUID());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(UUID.randomUUID(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("저장된 장소가 루트가 속한 플랜에 저장된 장소가 아니라면 루트에 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenPinPlaceIsNotInRoutePlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(UUID.randomUUID());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("저장된 장소를 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("저장할 장소를 지정하지 않으면 장소를 저장할 수 없고, 400을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whendPlanIㅇsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("pinPlaceId: 저장 할 장소는 생략이 불가능합니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("장소 별명이 2자 미만이면 장소를 저장할 수 없고, 400을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenNicknameIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 장소 별명은 2 ~ 20자여야 합니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("장소 별명이 20자 초과면 장소를 저장할 수 없고, 400을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenNicknameIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙".repeat(21))
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 장소 별명은 2 ~ 20자여야 합니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("장소 설명이 2자 미만이면 장소를 저장할 수 없고, 400을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenDescriptionIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("1")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: 장소 메모는 2 ~ 50자여야 합니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("장소 설명이 50자 초과면 장소를 저장할 수 없고, 400을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenDescriptionIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("설".repeat(51))
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: 장소 메모는 2 ~ 50자여야 합니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("방문할 날짜를 지정하지 않으면 장소를 저장할 수 없고, 400을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenVisitDateIsMissing() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("date: 방문 할 날짜는 생략이 불가능합니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("유저가 존재하지 않으면 장소를 저장할 수 없고, 404를 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단된 유저는 장소를 저장할 수 없고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatuses")
    void shouldFailToSavePlaceToRoute_whenUserIsInactiveOrBanned(String status, UserStatus userStatus)
            throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

    private static Stream<Arguments> provideInactiveUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)

        );
    }

    @DisplayName("토큰이 만료되었으면 장소를 저장할 수 없고, 401을 반환한다.")
    @Test
    void shouldFailToSavePlaceToRoute_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        SaveRoutePlaceRequest request = SaveRoutePlaceRequest.builder()
                .pinPlaceId(pinPlace.getId())
                .description("11시 퇴실해야함")
                .nickname("숙소")
                .date(LocalDate.parse("2025-09-30"))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(100000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/routes/" + route.getId() + "/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        assertThat(routePlaceRepository.count()).isZero();
    }

}
