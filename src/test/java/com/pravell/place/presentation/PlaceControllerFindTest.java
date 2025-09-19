package com.pravell.place.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.place.domain.model.PinPlace;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlaceControllerFindTest extends PlaceControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        pinPlaceRepository.deleteAllInBatch();
    }

    @Value("${naver.map.url}")
    private String mapUrl;

    @DisplayName("장소가 저장된 플랜이 PUBLIC이고, OWNER, MEMBER, 비참여자일 경우 장소 상세 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한: {0}")
    @MethodSource("provideAccessStatusesForPublicPlanPlaceDetailAccess")
    void shouldGetPlaceDetails_whenPublicPlanAndProperAccess(String role, PlanUserStatus planUserStatus)
            throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(pinPlace.getNickname()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.description").value(pinPlace.getDescription()))
                .andExpect(jsonPath("$.pinColor").value(pinPlace.getPinColor()))
                .andExpect(jsonPath("$.hours[0]").value("Monday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.hours[1]").value("Tuesday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.mapUrl").value(mapUrl+pinPlace.getTitle()));
    }

    private static Stream<Arguments> provideAccessStatusesForPublicPlanPlaceDetailAccess() {
        return Stream.of(
                Arguments.of("OWNER", PlanUserStatus.OWNER),
                Arguments.of("MEMBER", PlanUserStatus.MEMBER),
                Arguments.of("KICKED", PlanUserStatus.KICKED),
                Arguments.of("WITHDRAWN", PlanUserStatus.WITHDRAWN),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("PUBLIC 플랜에서 차단된 유저일 경우, 장소 상세 조회에 실패하고 403을 반환한다.")
    @Test
    void shouldReturnPlaceDetails_whenBlockedUserAccessesPublicPlan() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.BLOCKED);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));

    }

    @DisplayName("장소가 저장된 플랜이 PRIVATE이고,  OWNER, MEMBER일 경우 장소 상세 조회에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한: {0}")
    @MethodSource("providePrivatePlanAccessStatusesForPlaceDetail")
    void shouldGetPlaceDetails_whenPrivatePlanAndUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus)
            throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value(pinPlace.getNickname()))
                .andExpect(jsonPath("$.address").value(pinPlace.getAddress()))
                .andExpect(jsonPath("$.description").value(pinPlace.getDescription()))
                .andExpect(jsonPath("$.pinColor").value(pinPlace.getPinColor()))
                .andExpect(jsonPath("$.hours[0]").value("Monday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.hours[1]").value("Tuesday: 10:00 AM – 9:00 PM"))
                .andExpect(jsonPath("$.mapUrl").value(mapUrl+pinPlace.getTitle()));
    }

    private static Stream<Arguments> providePrivatePlanAccessStatusesForPlaceDetail() {
        return Stream.of(
                Arguments.of("OWNER", PlanUserStatus.OWNER),
                Arguments.of("MEMBER", PlanUserStatus.MEMBER)
        );
    }

    @DisplayName("장소가 저장된 플랜이 PRIVATE이고,  KICKED, WITHDRAWN, BLOCKED, 비참여자일 경우 장소 상세 조회에 실패하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한: {0}")
    @MethodSource("provideUnauthorizedStatusesForPrivatePlanAccess")
    void shouldReturn403_whenUserIsUnauthorizedForPrivatePlanPlaceDetail(String role, PlanUserStatus planUserStatus)
            throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    private static Stream<Arguments> provideUnauthorizedStatusesForPrivatePlanAccess() {
        return Stream.of(
                Arguments.of("KICKED", PlanUserStatus.KICKED),
                Arguments.of("WITHDRAWN", PlanUserStatus.WITHDRAWN),
                Arguments.of("BLOCKED", PlanUserStatus.BLOCKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("장소가 존재하지 않으면 장소 상세 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenPlaceDoesNotExist() throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + 10L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다."));
    }

    @DisplayName("플랜이 삭제되었으면 않으면 장소 상세 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingPlaceInDeletedPlan() throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true, false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("플랜이 존재하지 않으면 장소 상세 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenAccessingPlaceInNonExistentPlan() throws Exception {
        // given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        PinPlace pinPlace = getPinPlace(UUID.randomUUID());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("유저가 존재하지 않으면 장소 상세 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExistWhileAccessingPlaceDetails() throws Exception {
        // given
        Plan plan = getPlan(false, false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), UUID.randomUUID(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(UUID.randomUUID());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("탈퇴한 유저, 삭제된 유저, 정지된 유저, 차단된 유저는 플랜 상세 조회에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한: {0}")
    @MethodSource("provideInactiveUserStatusesForPublicPlanAccess")
    void shouldReturn404_whenUserIsInactiveOrBlockedForPublicPlanPlace(String role, UserStatus userStatus)
            throws Exception {
        // given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false, true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        PinPlace pinPlace = getPinPlace(plan.getId());
        pinPlaceRepository.save(pinPlace);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        // when, then
        mockMvc.perform(
                        get("/api/v1/places/" + pinPlace.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideInactiveUserStatusesForPublicPlanAccess() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("차단된 유저", UserStatus.BLOCKED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("삭제된 유저", UserStatus.DELETED)
        );
    }

}
