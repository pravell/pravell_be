package com.pravell.place.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class PlaceControllerFindAllTest extends PlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 MEMBER는 PRIVATE 플랜의 저장된 장소 목록을 조회할 수 있다.")
    @Test
    void shouldReturnPlaceList_whenMemberAccessesPrivatePlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor()),
                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor()),
                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor())
                );
    }

    @DisplayName("플랜의 OWNER는 PRIVATE 플랜의 저장된 장소 목록을 조회할 수 있다.")
    @Test
    void shouldReturnPlaceList_whenOwnerAccessesPrivatePlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor()),
                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor()),
                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor())
                );
    }

    @DisplayName("플랜에 참여하지 않은 유저는 PRIVATE 플랜의 저장된 장소 목록을 조회할 수 없고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenUnrelatedUserTriesToAccessPrivatePlanPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));

    }

    @DisplayName("플랜의 OWNER는 PUBLIC 플랜의 저장된 장소 목록을 조회할 수 있다.")
    @Test
    void shouldReturnPlaceList_whenOwnerAccessesPublicPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor()),
                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor()),
                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor())
                );
    }

    @DisplayName("플랜에 참여하지 않은 멤버는 PUBLIC 플랜의 저장된 장소 목록을 조회할 수 있다.")
    @Test
    void shouldReturnPlaceList_whenNonMemberAccessesPublicPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor()),
                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor()),
                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor())
                );
    }

    @DisplayName("플랜에서 탈퇴한 멤버는 PUBLIC 플랜의 저장된 장소 목록을 조회할 수 있다.")
    @Test
    void shouldReturnPlaceList_whenWithdrawnUserAccessesPublicPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor()),
                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor()),
                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor())
                );
    }

    @DisplayName("플랜에서 강퇴당한 멤버는 PUBLIC 플랜의 저장된 장소 목록을 조회할 수 있다.")
    @Test
    void shouldReturnPlaceList_whenKickedUserAccessesPublicPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.KICKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor()),
                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor()),
                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor())
                );
    }

    @DisplayName("플랜에서 탈퇴한 멤버는 PRIVATE 플랜의 저장된 장소 목록을 조회할 수 없고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenWithdrawnUserAccessesPrivatePlanPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.WITHDRAWN);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("플랜에서 강퇴당한 멤버는 PRIVATE 플랜의 저장된 장소 목록을 조회할 수 없고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenKickedUserAccessesPrivatePlanPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.KICKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("플랜에서 차단된 유저는 PRIVATE 플랜의 저장된 장소 목록을 조회할 수 없고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedUserTriesToAccessPrivatePlanPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("플랜에서 차단된 유저는 PUBLIC 플랜의 저장된 장소 목록을 조회할 수 없고, 403을 반환한다.")
    @Test
    void shouldReturn403_whenBlockedUserTriesToAccessPublicPlanPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    @DisplayName("존재하지 않는 플랜은 저장된 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingPlacesOfNonExistentPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + UUID.randomUUID())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("삭제된 플랜은 저장된 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingPlacesOfDeletedPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("유저가 존재하지 않으면 저장된 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingPlacesWithNonExistentUser() throws Exception {
        //given
        Plan plan = getPlan(true, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("탈퇴한 유저는 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnUserTriesToAccessPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.WITHDRAWN);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("삭제된 유저는 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedUserTriesToAccessPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.DELETED);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("정지된 유저는 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedUserTriesToAccessPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.SUSPENDED);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("차단된 유저는 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedUserTriesToAccessPlaces() throws Exception {
        //given
        User user = getUser(UserStatus.BLOCKED);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        Plan plan2 = getPlan(false, true);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }


}
