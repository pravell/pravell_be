package com.pravell.marker.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.pravell.marker.domain.model.Marker;
import com.pravell.marker.presentation.request.CreateMarkerRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
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
import org.springframework.test.web.servlet.MvcResult;

class MarkerControllerCreateTest extends MarkerControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        markerRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 멤버, 소유자라면 마커 생성에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideOwnerAndMemberRoles")
    void shouldCreateMarker_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(plan.getId())
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.markerId").isNotEmpty())
                .andExpect(jsonPath("$.planId").value(plan.getId().toString()))
                .andExpect(jsonPath("$.color").value(request.getColor()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(content);
        Long markerId = jsonNode.get("markerId").asLong();

        assertThat(markerRepository.count()).isOne();
        Optional<Marker> after = markerRepository.findById(markerId);
        assertThat(after).isPresent();
        assertThat(after.get().getPlanId()).isEqualTo(request.getPlanId());
        assertThat(after.get().getColor()).isEqualTo(request.getColor());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideOwnerAndMemberRoles() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴, 퇴출, 차단, 비참여 유저면 마커 생성에 실패하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideBlockedOrUnrelatedStatuses")
    void shouldReturn403_whenBlockedOrUnrelatedUserTriesToCreateMarker(String role, PlanUserStatus planUserStatus)
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

        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(plan.getId())
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 마커를 생성 할 권한이 없습니다."));

        assertThat(markerRepository.count()).isZero();
    }

    private static Stream<Arguments> provideBlockedOrUnrelatedStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 멤버", PlanUserStatus.WITHDRAWN),
                Arguments.of("퇴출당한 멤버", PlanUserStatus.KICKED),
                Arguments.of("차단된 멤버", PlanUserStatus.BLOCKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("탈퇴한 유저, 차단당한 유저, 삭제된 유저, 정지된 유저일 경우, 마커 생성에 실패하고 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideInvalidUserStatusesForMarkerCreate")
    void shouldReturn404_whenInvalidUserTriesToCreateMarker(String role, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(plan.getId())
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(markerRepository.count()).isZero();
    }

    private static Stream<Arguments> provideInvalidUserStatusesForMarkerCreate() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("차단당한 유저", UserStatus.BLOCKED),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED)
        );
    }

    @DisplayName("유저가 존재하지 않으면, 마커 생성에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(plan.getId())
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("플랜이 존재하지 않으면 마커 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("플랜이 삭제되었으면 마커 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(plan.getId())
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("마커 설명이 null이면 마커 생성에 실패하고, 400을반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsNull() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .color("#61EB52")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은 생략이 불가능합니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("마커 설명이 공백이면 마커 생성에 실패하고, 400을반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsBlank() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .color("#61EB52")
                .description("  ")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은 생략이 불가능합니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("plan Id가 공백이면 마커 생성에 실패하고, 400을반환한다.")
    @Test
    void shouldReturn400_whenPlanIdIsNull() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .color("#61EB52")
                .description("숙소")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("planId: planId는 생략이 불가능합니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("색상이 Null이면 마커 생성에 실패하고, 400을반환한다.")
    @Test
    void shouldReturn400_whenColorIsNull() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .description("숙소")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("color: pin color는 생략이 불가능합니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("색상이 HEX code 형식이 아니면 마커 생성에 실패하고, 400을반환한다.")
    @Test
    void shouldReturn400_whenColorIsNotHexCode() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .description("숙소")
                .color("1234")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("color: 올바른 HEX 색상 코드 형식이 아닙니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("토큰이 올바르지 않으면 마커 생성에 실패하고, 401을반환한다.")
    @Test
    void shouldReturn401_whenTokenIsInvalid() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .description("숙소")
                .color("#61EB52")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000)) + "aa";

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        assertThat(markerRepository.count()).isZero();
    }

    @DisplayName("토큰이 만료되었으면 마커 생성에 실패하고, 401을반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpired() throws Exception {
        //given
        CreateMarkerRequest request = CreateMarkerRequest.builder()
                .planId(UUID.randomUUID())
                .description("숙소")
                .color("#61EB52")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().minusSeconds(1000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/markers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        assertThat(markerRepository.count()).isZero();
    }

}
