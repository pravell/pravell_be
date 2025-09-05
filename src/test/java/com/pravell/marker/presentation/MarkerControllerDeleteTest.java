package com.pravell.marker.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.marker.domain.model.Marker;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
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

class MarkerControllerDeleteTest extends MarkerControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        markerRepository.deleteAllInBatch();
    }

    @DisplayName("마커가 저장된 플랜의 멤버, 소유자는 마커를 삭제할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideOwnerAndMemberStatuses")
    void shouldDeleteMarker_whenUserIsOwnerOrMemberOfPlan(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        //then
        assertThat(markerRepository.count()).isZero();
        assertThat(markerRepository.findById(marker.getId())).isNotPresent();
    }

    private static Stream<Arguments> provideOwnerAndMemberStatuses() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("마커가 저장된 플랜의 탈퇴, 차단, 퇴출, 비참여자 유저는 마커를 삭제할 수 없고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanMemberStatuses")
    void shouldReturn403_whenUnauthorizedUserTriesToDeleteMarker(String role, PlanUserStatus planUserStatus)
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

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("마커를 삭제할 권한이 없습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    private static Stream<Arguments> provideUnauthorizedPlanMemberStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("퇴출당한 유저", PlanUserStatus.KICKED),
                Arguments.of("비참여 유저", null)
        );
    }

    @DisplayName("마커가 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenMarkerDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/markers/" + 1000)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("마커를 찾을 수 없습니다."));
    }

    @DisplayName("마커가 저장된 플랜이 존재하지 않으면 마커 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanDoesNotExistWhileDeletingMarker() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Marker marker = getMarker(UUID.randomUUID(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    @DisplayName("마커가 저장된 플랜이 삭제되었으면 마커 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanIsDeletedWhileDeletingMarker() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    @DisplayName("유저가 존재하지 않으면 마커 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExistWhileDeletingMarker() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    @DisplayName("유저가 탈퇴, 삭제, 정지, 차단되었으면 마커 삭제에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveOrBlockedUserStatuses")
    void shouldReturn404_whenUserIsInactiveOrBlockedWhileDeletingMarker(String status, UserStatus userStatus)
            throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    private static Stream<Arguments> provideInactiveOrBlockedUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("토큰이 만료되었으면 마커 삭제에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpiredWhileDeletingMarker() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().minusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    @DisplayName("토큰이 변조되었으면 마커 삭제에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsTamperedWhileDeletingMarker() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000)) + "aa";

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    @DisplayName("Access Token이 아니라면 마커 삭제에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsNotAccessTokenWhileDeletingMarker() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(UUID.randomUUID(), "refresh", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

    @DisplayName("Bearer 없이 토큰이 전송되었다면 마커 삭제에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsNotAccessToken() throws Exception {
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        Marker marker = getMarker(plan.getId(), "#123456", "숙소");
        markerRepository.save(marker);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();

        //when
        mockMvc.perform(
                        delete("/api/v1/markers/" + marker.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(markerRepository.count()).isOne();
        assertThat(markerRepository.findById(marker.getId())).isPresent();
    }

}
