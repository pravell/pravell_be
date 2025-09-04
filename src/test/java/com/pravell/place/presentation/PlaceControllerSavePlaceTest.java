package com.pravell.place.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.ControllerTestSupport;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.place.presentation.request.SavePlaceRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class PlaceControllerSavePlaceTest extends ControllerTestSupport {

    @Autowired
    private PinPlaceRepository pinPlaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    @AfterEach
    void tearDown() {
        pinPlaceRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 MEMBER일 경우, 플랜에 성공적으로 장소를 저장한다.")
    @Test
    void shouldSavePlaceToPlan_whenUserIsMember() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinPlaceId").isNotEmpty())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long placeId = jsonNode.get("pinPlaceId").asLong();

        Optional<PinPlace> after = pinPlaceRepository.findById(placeId);
        assertThat(after).isPresent();
        assertThat(after.get().getAddress()).isEqualTo(request.getAddress());
        assertThat(after.get().getSavedUser()).isEqualTo(user.getId());
        assertThat(after.get().getPlanId()).isEqualTo(plan.getId());
    }

    @DisplayName("플랜의 OWNER일 경우, 플랜에 성공적으로 장소를 저장한다.")
    @Test
    void shouldSavePlaceToPlan_whenUserIsOwner() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinPlaceId").isNotEmpty())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long placeId = jsonNode.get("pinPlaceId").asLong();

        Optional<PinPlace> after = pinPlaceRepository.findById(placeId);
        assertThat(after).isPresent();
        assertThat(after.get().getAddress()).isEqualTo(request.getAddress());
        assertThat(after.get().getSavedUser()).isEqualTo(user.getId());
        assertThat(after.get().getPlanId()).isEqualTo(plan.getId());
    }

    @DisplayName("플랜에 속해있지 않은 유저는 플랜에 장소를 저장하지 못하고, 403을 반환한다.")
    @Test
    void shouldThrowAccessDeniedException_whenUserNotInPlanTriesToSavePlace() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 장소를 저장 할 권한이 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("플랜에서 탈퇴한 유저는 장소를 저장하지 못하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenWithdrawnUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 장소를 저장 할 권한이 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("플랜에서 퇴출당한 유저는 장소를 저장하지 못하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenKickedUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.KICKED);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 장소를 저장 할 권한이 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("플랜에서 차단당한 유저는 장소를 저장하지 못하고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 플랜에 장소를 저장 할 권한이 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("해당 장소가 이미 저장되어있으면 장소를 중복해서 저장하지 못하고, 409를 반환한다.")
    @Test
    void shouldNotSavePlace_whenPlaceAlreadyExistsInPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        pinPlaceRepository.save(getPinPlace(request.getAddress(), plan.getId()));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        assertThat(pinPlaceRepository.count()).isOne();

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("해당 장소는 이미 저장되어 있습니다."));

        assertThat(pinPlaceRepository.count()).isOne();
    }

    @DisplayName("해당 플랜이 존재하지 않으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSavingPlaceToNonExistentPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", UUID.randomUUID());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("해당 플랜이 이미 삭제되었으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSavingPlaceToDeletedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("유저가 존재하지 않으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSavingPlaceWithNonExistentUser() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), UUID.randomUUID(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("유저가 이미 탈퇴했으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.WITHDRAWN);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("유저가 이미 삭제되었으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.DELETED);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("유저가 이미 정지되었으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.SUSPENDED);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("유저가 이미 차단되었으면 장소를 저장하지 못하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedUserTriesToSavePlaceToPlan() throws Exception {
        //given
        User user = getUser(UserStatus.BLOCKED);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        SavePlaceRequest request = getSavePlaceRequest("경상북도 경주시 탑동 xxx", plan.getId());

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    private User getUser(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .userId("userId" + UUID.randomUUID())
                .nickname("nickname" + UUID.randomUUID())
                .password("passworddd")
                .status(status)
                .build();
    }

    private Plan getPlan(boolean isDeleted) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("name")
                .isDeleted(isDeleted)
                .isPublic(true)
                .build();
    }

    private PlanUsers getPlanUsers(UUID planId, UUID userId, PlanUserStatus status) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

    private SavePlaceRequest getSavePlaceRequest(String address, UUID planId) {
        return SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("title")
                .address(address)
                .roadAddress(address + " road")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(planId)
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();
    }

    private PinPlace getPinPlace(String address, UUID planId) {
        return PinPlace.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("title")
                .address(address)
                .roadAddress(address + " road")
                .hours("hours")
                .mapy("12345")
                .mapy("123456")
                .pinColor("#F54927")
                .planId(planId)
                .savedUser(UUID.randomUUID())
                .description("description")
                .lastRefreshedAt(LocalDateTime.now())
                .latitude(new BigDecimal("123.44567"))
                .longitude(new BigDecimal("123.23456"))
                .build();
    }

}
