package com.pravell.route.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.place.domain.model.PinPlace;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.presentation.request.UpdatePlaceRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

class RoutePlaceControllerUpdateTest extends RoutePlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        routePlaceRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();;
        routePlaceRepository.deleteAllInBatch();
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜의 멤버, 소유자라면 장소를 업데이트 할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedRolesForRoutePlaceUpdate")
    void shouldUpdateRoutePlace_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routePlaceId").value(routePlace.getId().toString()))
                .andExpect(jsonPath("$.pinPlaceId").value(request.getPinPlaceId().toString()))
                .andExpect(jsonPath("$.title").value(updatePinPlace.getTitle()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.sequence").value(request.getSequence()))
                .andExpect(jsonPath("$.date").value(request.getDate().toString()))
                .andExpect(jsonPath("$.address").value(updatePinPlace.getAddress()))
                .andExpect(jsonPath("$.roadAddress").value(updatePinPlace.getRoadAddress()))
                .andExpect(jsonPath("$.mapx").value(updatePinPlace.getMapx()))
                .andExpect(jsonPath("$.mapy").value(updatePinPlace.getMapy()))
                .andExpect(jsonPath("$.lat").value(updatePinPlace.getLatitude()))
                .andExpect(jsonPath("$.lng").value(updatePinPlace.getLongitude()))
                .andExpect(jsonPath("$.color").value(updatePinPlace.getPinColor()));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isEqualTo(request.getSequence());
        assertThat(after.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideAuthorizedRolesForRoutePlaceUpdate() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜에서 탈퇴, 강퇴, 차단당했거나 비참여 유저라면 장소를 업데이트에 실패하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedRolesForRoutePlaceUpdate")
    void shouldFailToUpdateRoutePlace_whenUserIsNotParticipantOrBanned(String role, PlanUserStatus planUserStatus)
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

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 수정 할 권한이 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    private static Stream<Arguments> provideUnauthorizedRolesForRoutePlaceUpdate() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("루트에 해당 장소가 저장되어있지 않으면 장소 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenPlaceNotInRoute() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + 1000)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트에서 해당 장소를 찾을 수 없습니다."));

        //then
        assertThat(routePlaceRepository.count()).isZero();
    }

    @DisplayName("장소가 저장된 루트가 속한 플랜이 삭제되었다면 장소를 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("장소가 저장된 루트 속한 플랜이 존재하지 않으면 장소를 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Route route = getRoute(UUID.randomUUID(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(UUID.randomUUID());
        pinPlaceRepository.save(pinPlace);

        RoutePlace routePlace = getRoutePlace(UUID.randomUUID(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(10L)
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("변경할 pinPlace가 플랜에 저장되어있지 않으면 장소를 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenPlaceIsNotSavedInPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(10000L)
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("저장된 장소를 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("기존에 있던 pinPlace가 삭제되었다면 장소를 업데이트에 실패하고, 403을 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenPinPlaceIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.save(updatePinPlace);

        RoutePlace routePlace = getRoutePlace(route.getId(), 1000L, 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("저장된 장소를 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("장소가 저장된 루트가 삭제되었다면 장소를 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenRouteIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), true);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("장소가 저장된 루트가 존재하지 않으면 장소를 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenRouteDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(UUID.randomUUID(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + UUID.randomUUID() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("루트를 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단당한 유저는 장소를 업데이트에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveOrBannedUserStatuses")
    void shouldFailToUpdateRoutePlace_whenUserIsInactiveOrBanned(String status, UserStatus userStatus)
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

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    private static Stream<Arguments> provideInactiveOrBannedUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)

        );
    }

    @DisplayName("유저가 존재하지 않으면 장소를 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("토큰이 만료되었으면 장소를 업데이트에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

    @DisplayName("토큰이 변조되었으면 장소를 업데이트에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToUpdateRoutePlace_whenTokenIsTampered() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Route route = getRoute(plan.getId(), false);
        routeRepository.save(route);

        PinPlace pinPlace = getPinPlace(plan.getId());
        PinPlace updatePinPlace = getPinPlace(plan.getId(), "변경할 장소 이름", "변경할 장소 주소", "변경할 장소 도로명 주소", "098765",
                "123456", "#098765");
        pinPlaceRepository.saveAll(List.of(pinPlace, updatePinPlace));

        RoutePlace routePlace = getRoutePlace(route.getId(), pinPlace.getId(), 1L, "장소 설명", "장소 별명",
                LocalDate.parse("2025-09-10"));
        routePlaceRepository.save(routePlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000))+"aa";

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinPlaceId(updatePinPlace.getId())
                .nickname("업데이트 할 장소 별명")
                .description("업데이트 할 장소 설명")
                .date(LocalDate.parse("2025-09-30"))
                .sequence(10L)
                .build();

        Optional<RoutePlace> before = routePlaceRepository.findById(routePlace.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(before.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(before.get().getDescription()).isNotEqualTo(request.getDescription());

        //when
        mockMvc.perform(
                        patch("/api/v1/routes/" + route.getId() + "/places/" + routePlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<RoutePlace> after = routePlaceRepository.findById(routePlace.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getSequence()).isNotEqualTo(request.getSequence());
        assertThat(after.get().getSequence()).isEqualTo(routePlace.getSequence());
        assertThat(after.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(after.get().getNickname()).isEqualTo(routePlace.getNickname());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(routePlace.getDescription());
    }

}

