package com.pravell.marker.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.marker.domain.model.Marker;
import com.pravell.marker.presentation.request.UpdateMarkerRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
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

class MarkerControllerUpdateTest extends MarkerControllerTestSupport {

    @DisplayName("마커가 저장된 플랜의 멤버, 소유자는 마커 수정이 가능하다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideOwnerAndMemberStatuses")
    void shouldUpdateMarker_whenUserIsOwnerOrMemberOfPlan(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markerId").value(marker.getId()))
                .andExpect(jsonPath("$.planId").value(marker.getPlanId().toString()))
                .andExpect(jsonPath("$.color").value(request.getColor()))
                .andExpect(jsonPath("$.description").value(request.getDescription()));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isEqualTo(request.getColor());
    }

    private static Stream<Arguments> provideOwnerAndMemberStatuses() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("마커가 저장된 플랜의 탈퇴, 차단, 퇴출, 비참여 유저는 마커 수정이 불가능하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedStatusesForMarkerUpdate")
    void shouldReturn403_whenUnauthorizedUserTriesToUpdateMarker(String role, PlanUserStatus planUserStatus)
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

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("마커를 수정 할 권한이 없습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    private static Stream<Arguments> provideUnauthorizedStatusesForMarkerUpdate() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단된 유저", PlanUserStatus.BLOCKED),
                Arguments.of("퇴출당한 유저", PlanUserStatus.KICKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("변경할 마커 색상이 HEX code 형식이 아니라면 마커 수정에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenColorIsNotHex() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("123")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("color: 올바른 HEX 색상 코드 형식이 아닙니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("변경할 마커 설명이 2자 미만이면 마커 수정에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은 2 ~ 30자여야 합니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("변경할 마커 설명이 30자 초과면 마커 수정에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카" .repeat(31))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은 2 ~ 30자여야 합니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("마커가 존재하지 않으면 마커 수정에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenMarkerDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/markers/" + 100)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("마커를 찾을 수 없습니다."));
    }

    @DisplayName("마커가 저장된 플랜이 존재하지 않으면 마커 수정에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanDoesNotExistForMarkerUpdate() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(UUID.randomUUID(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("마커가 저장된 플랜이 삭제되었으면 마커 수정에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn404_whenUpdatingMarkerWithDeletedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("유저가 존재하지 않으면 마커 수정에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUpdatingMarkerWithNonExistentUser() throws Exception {
        Plan plan = getPlan(true);
        planRepository.save(plan);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("유저가 탈퇴, 삭제, 정지, 차단된 유저라면 마커 수정에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideWithdrawnOrDeactivatedUserStatuses")
    void shouldReturn404_whenInvalidUserTriesToUpdateMarker(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    private static Stream<Arguments> provideWithdrawnOrDeactivatedUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("토큰이 만료되었으면 마커 수정에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpiredWhileUpdatingMarker() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("토큰이 변조되었으면 마커 수정에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsTamperedWhileUpdatingMarker() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000))+"aa";

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

    @DisplayName("Access Token이 아니라면 마커 수정에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsNotAccessTokenWhileUpdatingMarker() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        UpdateMarkerRequest request = UpdateMarkerRequest.builder()
                .color("#098765")
                .description("카페")
                .build();

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().plusSeconds(10000));

        Optional<Marker> before = markerRepository.findById(marker.getId());
        assertThat(before).isPresent();
        assertThat(before.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(before.get().getColor()).isEqualTo(marker.getColor());

        //when
        mockMvc.perform(
                        patch("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Marker> after = markerRepository.findById(marker.getId());
        assertThat(after).isPresent();
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(after.get().getColor()).isNotEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(marker.getDescription());
        assertThat(after.get().getColor()).isEqualTo(marker.getColor());
    }

}
