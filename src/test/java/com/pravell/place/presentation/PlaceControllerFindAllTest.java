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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StringUtils;

class PlaceControllerFindAllTest extends PlaceControllerTestSupport {

    @Value("${naver.map.url}")
    private String mapUrl;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("PRIVATE 플랜에 저장된 장소 목록은 오너 및 멤버만 조회할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideOwnerAndMemberStatuses")
    void shouldReturnPlaceList_whenUserIsOwnerOrMemberOfPrivatePlan(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace1 = getPinPlace("add1", plan.getId());
        PinPlace pinPlace2 = getPinPlace("add2", plan.getId());
        PinPlace pinPlace3 = getPinPlace("add3", plan.getId());

        PinPlace pinPlace4 = getPinPlace("add4", plan2.getId());
        PinPlace pinPlace5 = getPinPlace("add5", plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/plan/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor", "address", "roadAddress", "hours", "mapUrl")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor(),
                                pinPlace1.getAddress(), pinPlace1.getRoadAddress(), parseHours(pinPlace1.getHours()),
                                mapUrl + pinPlace1.getTitle()),

                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor(),
                                pinPlace2.getAddress(), pinPlace2.getRoadAddress(), parseHours(pinPlace2.getHours()),
                                mapUrl + pinPlace2.getTitle()),

                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor(),
                                pinPlace3.getAddress(), pinPlace3.getRoadAddress(), parseHours(pinPlace3.getHours()),
                                mapUrl + pinPlace3.getTitle())
                );
    }

    private static Stream<Arguments> provideOwnerAndMemberStatuses() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("오너", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("탈퇴, 퇴출, 차단, 비참여자는 PRIVATE 플랜의 장소 목록을 조회할 수 없다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideInvalidStatusesForPrivatePlaceListAccess")
    void shouldReturn403_whenUnauthorizedUserTriesToAccessPrivatePlaceList(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        Plan plan2 = getPlan(false, false);
        planRepository.saveAll(List.of(plan, plan2));

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/plan/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    private static Stream<Arguments> provideInvalidStatusesForPrivatePlaceListAccess() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("퇴출당한 유저", PlanUserStatus.KICKED),
                Arguments.of("차단된 유저", PlanUserStatus.BLOCKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("PUBLIC 플랜은 OWNER, MEMBER, 탈퇴자, 강퇴자, 비참여자가 장소 목록을 조회할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAllStatusesForPublicPlaceListAccess")
    void shouldReturnPlaceList_whenUserAccessesPublicPlan(String role, PlanUserStatus planUserStatus) throws Exception {
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

        PinPlace pinPlace1 = getPinPlace(plan.getId());
        PinPlace pinPlace2 = getPinPlace(plan.getId());
        PinPlace pinPlace3 = getPinPlace(plan.getId());

        PinPlace pinPlace4 = getPinPlace(plan2.getId());
        PinPlace pinPlace5 = getPinPlace(plan2.getId());
        pinPlaceRepository.saveAll(List.of(pinPlace1, pinPlace2, pinPlace3, pinPlace4, pinPlace5));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/places/plan/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        List<Map<String, Object>> responseList = objectMapper.readValue(content, new TypeReference<>() {
        });

        //then
        assertThat(responseList).hasSize(3)
                .extracting("title", "mapx", "mapy", "pinColor", "address", "roadAddress", "hours", "mapUrl")
                .containsExactlyInAnyOrder(
                        tuple(pinPlace1.getTitle(), pinPlace1.getMapx(), pinPlace1.getMapy(), pinPlace1.getPinColor(),
                                pinPlace1.getAddress(), pinPlace1.getRoadAddress(), parseHours(pinPlace1.getHours()),
                                mapUrl + pinPlace1.getTitle()),

                        tuple(pinPlace2.getTitle(), pinPlace2.getMapx(), pinPlace2.getMapy(), pinPlace2.getPinColor(),
                                pinPlace2.getAddress(), pinPlace2.getRoadAddress(), parseHours(pinPlace2.getHours()),
                                mapUrl + pinPlace2.getTitle()),

                        tuple(pinPlace3.getTitle(), pinPlace3.getMapx(), pinPlace3.getMapy(), pinPlace3.getPinColor(),
                                pinPlace3.getAddress(), pinPlace3.getRoadAddress(), parseHours(pinPlace3.getHours()),
                                mapUrl + pinPlace3.getTitle())
                );
    }

    private static Stream<Arguments> provideAllStatusesForPublicPlaceListAccess() {
        return Stream.of(
                Arguments.of("소유자", PlanUserStatus.OWNER),
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("탈퇴한 유저", PlanUserStatus.WITHDRAWN),
                Arguments.of("강퇴당한 유저", PlanUserStatus.KICKED),
                Arguments.of("비참여자", null)
        );
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
                        get("/api/v1/places/plan/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
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
                        get("/api/v1/places/plan/" + plan.getId())
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
                        get("/api/v1/places/plan/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("탈퇴, 삭제, 정지, 차단당한 유저는 장소 목록을 조회할 수 없고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("invalidUserStatusesForPlaceListAccess")
    void shouldReturn404_whenInvalidUserTriesToAccessPlaceList(String role, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
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
                        get("/api/v1/places/plan/" + plan.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> invalidUserStatusesForPlaceListAccess() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    private List<String> parseHours(String hours) throws Exception {
        List<String> hoursList = new ArrayList<>();

        if (StringUtils.hasText(hours) && !hours.equals("정보 없음")) {
            hoursList = objectMapper.readValue(hours, new TypeReference<>() {
            });
        } else {
            hoursList.add("정보 없음");
        }

        return hoursList;
    }

}
