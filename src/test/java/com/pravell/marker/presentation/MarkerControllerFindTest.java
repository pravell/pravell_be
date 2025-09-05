package com.pravell.marker.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pravell.marker.application.dto.response.FindMarkersResponse;
import com.pravell.marker.domain.model.Marker;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
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

class MarkerControllerFindTest extends MarkerControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        markerRepository.deleteAllInBatch();
    }

    @DisplayName("PRIVATE 플랜이면 멤버, 소유자만 마커 조회가 가능하다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideOwnerAndMemberStatuses")
    void shouldReturnMarkers_whenUserAccessesPublicPlan(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        List<FindMarkersResponse> responses = objectMapper.readValue(
                responseBody,
                new TypeReference<List<FindMarkersResponse>>() {
                }
        );

        //then
        assertThat(responses).hasSize(2)
                .extracting("color", "description")
                .containsExactlyInAnyOrder(
                        tuple(marker1.getColor(), marker1.getDescription()),
                        tuple(marker2.getColor(), marker2.getDescription())
                );
    }

    private static Stream<Arguments> provideOwnerAndMemberStatuses() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("PRIVATE 플랜의 탈퇴, 퇴출, 차단 비참여 유저는 마커 조회가 불가능하며, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideStatusesForPrivatePlanMarkerReadForbidden")
    void shouldReturn403_whenUnauthorizedUserAccessesPrivatePlanMarkers(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 마커를 조회할 권한이 없습니다."));
    }

    private static Stream<Arguments> provideStatusesForPrivatePlanMarkerReadForbidden() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("퇴출", PlanUserStatus.KICKED),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("PUBLIC 플랜의 멤버, 소유자, 탈퇴, 차단 비참여 유저는 마커 조회가 가능하다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideStatusesForPublicPlanMarkerReadSuccess")
    void shouldReturnMarkers_whenUserIsOwnerOrMemberOfPrivatePlan(String role, PlanUserStatus planUserStatus)
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

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();

        List<FindMarkersResponse> responses = objectMapper.readValue(
                responseBody,
                new TypeReference<List<FindMarkersResponse>>() {
                }
        );

        //then
        assertThat(responses).hasSize(2)
                .extracting("color", "description")
                .containsExactlyInAnyOrder(
                        tuple(marker1.getColor(), marker1.getDescription()),
                        tuple(marker2.getColor(), marker2.getDescription())
                );
    }

    private static Stream<Arguments> provideStatusesForPublicPlanMarkerReadSuccess() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER),
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("퇴출", PlanUserStatus.KICKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("PUBLIC 플랜은 차단된 유저는 마커 조회가 불가능하며, 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedUserAccessesPublicPlanMarkers() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 마커를 조회할 권한이 없습니다."));
    }

    @DisplayName("플랜이 존재하지 않으면 마커 조회가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanDoesNotExistForMarkerRead() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("플랜이 삭제되었으면 마커 조회가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingMarkersInDeletedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("유저가 존재하지 않으면 마커 조회가 불가능하며, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingMarkersWithNonExistentUser() throws Exception {
        //given
        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("유저가 탈퇴, 삭제, 정지, 차단되었으면 마커 조회가 불가능하며, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInvalidUserStatusesForMarkerRead")
    void shouldReturn404_whenAccessingMarkersWithInvalidUserStatus(String role, UserStatus userStatus)
            throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideInvalidUserStatusesForMarkerRead() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("토큰이 만료되었으면 마커 조회가 불가능하며, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsExpiredWhileReadingMarkers() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("토큰이 변조되었으면 마커 조회가 불가능하며, 401을 반환한다.")
    @Test
    void shouldReturn401_whenAccessTokenIsInvalidWhileReadingMarkers() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000)) + "aa";

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

    @DisplayName("access Token이 아니면 마커 조회가 불가능하며, 401을 반환한다.")
    @Test
    void shouldReturn401_whenNonAccessTokenUsedToReadMarkers() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Marker marker1 = getMarker(plan.getId(), "#123456", "마커 설명 1");
        Marker marker2 = getMarker(plan.getId(), "#234098", "마커 설명 2");

        Marker marker3 = getMarker(plan2.getId(), "#567890", "다른 플랜 마커");
        markerRepository.saveAll(List.of(marker1, marker2, marker3));

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/markers/" + plan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

}
