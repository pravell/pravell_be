package com.pravell.place.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.presentation.request.SavePlaceRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MvcResult;

class PlaceControllerSavePlaceTest extends PlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        pinPlaceRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 멤버, 오너일 경우, 플랜에 성공적으로 장소를 저장한다.")
    @ParameterizedTest
    @MethodSource("provideOwnerAndMemberStatuses")
    void shouldSavePlace_whenUserIsOwnerOrMemberOfPlan(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
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

    private static Stream<Arguments> provideOwnerAndMemberStatuses() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("오너", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴한 유저, 퇴출당한 유저, 차단된 유저, 비참여자일 경우 플랜에 장소를 저장하지 못하고 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedStatuses")
    void shouldReturn403_whenUnauthorizedUserTriesToSavePlace(String role, PlanUserStatus planUserStatus)
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

    private static Stream<Arguments> provideUnauthorizedStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("퇴출당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단된 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여자", null)
        );
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

    @DisplayName("탈퇴, 삭제, 정지, 차단된 유저는 장소 저장에 실패하고 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideInactiveUserStatuses")
    void shouldReturn404_whenInactiveUserTriesToSavePlace(String role, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
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

    private static Stream<Arguments> provideInactiveUserStatuses() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지당한 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
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

}
