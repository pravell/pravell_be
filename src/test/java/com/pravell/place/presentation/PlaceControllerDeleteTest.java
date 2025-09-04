package com.pravell.place.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.presentation.request.DeletePlacesRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlaceControllerDeleteTest extends PlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
    }

    @DisplayName("장소가 속한 플랜의 MEMBER일 경우, 장소를 삭제할 수 있다.")
    @Test
    void shouldDeletePlace_whenUserIsMemberOfPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                delete("/api/v1/places")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        //then
        assertThat(pinPlaceRepository.count()).isOne();
    }

    @DisplayName("장소가 속한 플랜의 OWNER일 경우, 장소를 삭제할 수 있다.")
    @Test
    void shouldDeletePlace_whenUserIsOwnerOfPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        //then
        assertThat(pinPlaceRepository.count()).isOne();
    }

    @DisplayName("장소가 속한 플랜에 속해있지 않을 경우, 장소를 삭제할 수 없고 403을 반환한다.")
    @Test
    void shouldDeletePlace_whenUserIsNotInPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 삭제 할 권한이 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("장소가 속한 플랜에서 퇴출되었을 경우, 장소를 삭제할 수 없고 403을 반환한다.")
    @Test
    void shouldDeletePlace_whenUserWasKickedFromPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.KICKED);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 삭제 할 권한이 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("장소가 속한 플랜에서 탈퇴했을 경우, 장소를 삭제할 수 없고 403을 반환한다.")
    @Test
    void shouldDeletePlace_whenUserWasWithdrawnFromPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 삭제 할 권한이 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("장소가 속한 플랜에서 차단당했을 경우, 장소를 삭제할 수 업고 403을 반환한다.")
    @Test
    void shouldDeletePlace_whenUserWasBlockedFromPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 장소를 삭제 할 권한이 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("장소가 속한 플랜이 삭제되었을 경우, 장소를 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletingPlaceFromDeletedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId()))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("플랜이 하나라도 존재하지 않을 경우, 장소를 전부 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenAnyPlanDoesNotExistWhileDeletingMultiplePlaces() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("유저가 존재하지 않을 경우, 장소를 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExistWhileDeletingMultiplePlaces() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), UUID.randomUUID(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }


    @DisplayName("유저가 이미 탈퇴했을 경우, 장소를 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserHasWithdrawnWhileDeletingMultiplePlaces() throws Exception {
        //given
        User user = getUser(UserStatus.WITHDRAWN);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("유저가 이미 삭제되었을 경우, 장소를 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserHasDeletedWhileDeletingMultiplePlaces() throws Exception {
        //given
        User user = getUser(UserStatus.DELETED);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("유저가 이미 정지되었을 경우, 장소를 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserHasSuspendedWhileDeletingMultiplePlaces() throws Exception {
        //given
        User user = getUser(UserStatus.SUSPENDED);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("유저가 이미 차단되었을 경우, 장소를 삭제하지 못하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserHasBlockedWhileDeletingMultiplePlaces() throws Exception {
        //given
        User user = getUser(UserStatus.BLOCKED);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("토큰이 만료되었을 경우, 장소를 삭제하지 못하고 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsExpiredWhileDeletingPlace() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

    @DisplayName("Access 토큰이 아닐 경우, 장소를 삭제하지 못하고 401을 반환한다.")
    @Test
    void shouldReturn401_whenTokenIsNotAccessTokenWhileDeletingPlace() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3));

        DeletePlacesRequest request = DeletePlacesRequest.builder()
                .placeId(List.of(pinPlace1.getId(), pinPlace2.getId(), 1110L))
                .build();

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().minusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isEqualTo(3);

        //when
        mockMvc.perform(
                        delete("/api/v1/places")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(pinPlaceRepository.count()).isEqualTo(3);
    }

}
