package com.pravell.place.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.presentation.request.UpdatePlaceRequest;
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

class PlaceControllerUpdateTest extends PlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("장소가 저장된 플랜의 OWNER, MEMBER일 경우 장소 업데이트에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한: {0}")
    @MethodSource("providePrivatePlanOwnerAndMemberStatuses")
    void shouldUpdatePlace_whenPrivatePlanAndUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus)
            throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("변경 할 장소 설명")
                .pinColor("#9C3E2C")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.pinColor").value(request.getPinColor()))
                .andExpect(jsonPath("$.hours[0]").value("Monday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.hours[1]").value("Tuesday: 10:00 AM – 9:00 PM"));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isEqualTo(request.getPinColor());
    }

    private static Stream<Arguments> providePrivatePlanOwnerAndMemberStatuses() {
        return Stream.of(
                Arguments.of("OWNER", PlanUserStatus.OWNER),
                Arguments.of("MEMBER", PlanUserStatus.MEMBER)
        );
    }

    @DisplayName("장소 별명만 업데이트 할 수 있다.")
    @Test
    void shouldUpdateOnlyNickname() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.description").value(pinPlace.getDescription()))
                .andExpect(jsonPath("$.pinColor").value(pinPlace.getPinColor()))
                .andExpect(jsonPath("$.hours[0]").value("Monday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.hours[1]").value("Tuesday: 10:00 AM – 9:00 PM"));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isEqualTo(pinPlace.getDescription());
        assertThat(place.get().getPinColor()).isEqualTo(pinPlace.getPinColor());
    }

    @DisplayName("장소 설명만 업데이트 할 수 있다.")
    @Test
    void shouldUpdateOnlyDescription() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .description("변경 할 장소 설명")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(pinPlace.getNickname()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.pinColor").value(pinPlace.getPinColor()))
                .andExpect(jsonPath("$.hours[0]").value("Monday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.hours[1]").value("Tuesday: 10:00 AM – 9:00 PM"));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isEqualTo(pinPlace.getNickname());
        assertThat(place.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isEqualTo(pinPlace.getPinColor());
    }

    @DisplayName("마커 색상만 업데이트 할 수 있다.")
    @Test
    void shouldUpdateOnlyPinColor() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .pinColor("#9C3E2C")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(pinPlace.getNickname()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.description").value(pinPlace.getDescription()))
                .andExpect(jsonPath("$.pinColor").value(request.getPinColor()))
                .andExpect(jsonPath("$.hours[0]").value("Monday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.hours[1]").value("Tuesday: 10:00 AM – 9:00 PM"));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isEqualTo(pinPlace.getNickname());
        assertThat(place.get().getDescription()).isEqualTo(pinPlace.getDescription());
        assertThat(place.get().getPinColor()).isEqualTo(request.getPinColor());
    }

    @DisplayName("장소가 저장된 플랜에서 차단당한 유저, 탈퇴한 유저, 퇴출당한 유저, 비참여자일 경우 장소 업데이트에 실패하고 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedUserStatusesForPlaceUpdate")
    void shouldReturn403_whenUnauthorizedUserTriesToUpdatePlace(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        if (planUserStatus!=null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("변경 할 장소 설명")
                .pinColor("#9C3E2C")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 수정 할 권한이 없습니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    private static Stream<Arguments> provideUnauthorizedUserStatusesForPlaceUpdate(){
        return Stream.of(
                Arguments.of("퇴출당한 유저", PlanUserStatus.KICKED),
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단당한 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("장소가 존재하지 않을 경우, 장소 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUpdatingNonExistentPlace() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("변경 할 장소 설명")
                .pinColor("#9C3E2C")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + 10)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다."));
    }

    @DisplayName("존재하지 않는 유저일 경우, 장소 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUpdatingPlaceWithNonExistentUser() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("변경 할 장소 설명")
                .pinColor("#9C3E2C")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    @DisplayName("탈퇴한 유저, 차단당한 유저, 삭제된 유저, 정지된 유저일 경우, 장소 업데이트에 실패하고 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideInvalidUserStatusesForPlaceUpdate")
    void shouldReturn404_whenUserIsInvalidOrNonExistent(String role, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("변경 할 장소 설명")
                .pinColor("#9C3E2C")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    private static Stream<Arguments> provideInvalidUserStatusesForPlaceUpdate(){
        return Stream.of(
                Arguments.of("탈퇴한 유저",UserStatus.WITHDRAWN),
                Arguments.of("차단당한 유저",UserStatus.BLOCKED),
                Arguments.of("삭제된 유저",UserStatus.DELETED),
                Arguments.of("정지된 유저",UserStatus.SUSPENDED)
        );
    }

    @DisplayName("변경 할 닉네임이 2자 미만일 경우, 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenNicknameIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("2")
                .description("변경 할 장소 설명")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: nickname은 2 ~ 30자여야 합니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    @DisplayName("변경 할 닉네임이 30자 초과일 경우, 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenNicknameIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("2".repeat(31))
                .description("변경 할 장소 설명")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: nickname은 2 ~ 30자여야 합니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    @DisplayName("변경 할 마커 색상이 HEX code 형식이 아닐 경우, 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenPinColorIsNotHexCode() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .pinColor("1234")
                .description("변경 할 장소 설명")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("pinColor: 올바르지 않은 pin color입니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    @DisplayName("변경 할 장소 설명이 2자 미만일 경우, 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenPlaceDescriptionIsTooShort() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("1")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은 2 ~ 255자여야 합니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

    @DisplayName("변경 할 장소 설명이 255자 초과일 경우, 변경에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenPlaceDescriptionIsTooLong() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        UpdatePlaceRequest request = UpdatePlaceRequest.builder()
                .nickname("변경 할 닉네임")
                .description("설".repeat(256))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        patch("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은 2 ~ 255자여야 합니다."));

        Optional<PinPlace> place = pinPlaceRepository.findById(pinPlace.getId());
        assertThat(place).isPresent();
        assertThat(place.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(place.get().getDescription()).isNotEqualTo(request.getDescription());
        assertThat(place.get().getPinColor()).isNotEqualTo(request.getPinColor());
    }

}
