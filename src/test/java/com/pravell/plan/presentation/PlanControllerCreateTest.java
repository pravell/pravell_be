package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pravell.ControllerTestSupport;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.plan.presentation.request.CreatePlanRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("플랜 생성 통합테스트")
class PlanControllerCreateTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    private static final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .id(userId)
                .userId("userIdddd")
                .password("password")
                .nickname("nickkkk")
                .status(UserStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("플랜을 성공적으로 생성한다.")
    @Test
    void shouldCreatePlanSuccessfully() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플랜 1", true);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId").isNotEmpty())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.isPublic").value(request.getIsPublic()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.startDate").value(request.getStartDate().toString()))
                .andExpect(jsonPath("$.endDate").value(request.getEndDate().toString()))
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        UUID planId = UUID.fromString(JsonPath.read(contentAsString, "$.planId"));

        Optional<Plan> plan = planRepository.findById(planId);
        assertThat(plan).isPresent();
        assertThat(plan.get().getName()).isEqualTo(request.getName());
        assertThat(plan.get().getStartDate()).isEqualTo(request.getStartDate());
    }

    @DisplayName("유저가 존재하지 않으면 여행 플랜 생성에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToCreatePlan_whenUserDoesNotExist() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플랜 1", true);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("플랜 이름이 2자 미만이면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToCreatePlan_whenNameIsTooShort() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플", true);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 플랜 이름은 2 ~ 20자 사이여야 합니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("플랜 이름이 20자 초과면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToCreatePlan_whenNameIsTooLong() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플".repeat(21), true);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 플랜 이름은 2 ~ 20자 사이여야 합니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("플랜 이름이 null이면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToCreatePlan_whenNameNull() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest(null, true);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 플랜 이름은 생략이 불가능합니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("플랜 이름이 공백이면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToCreatePlan_whenNameBlank() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest(" ", true);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("name: 플랜 이름은 2 ~ 20자 사이여야 합니다."),
                        containsString("name: 플랜 이름은 생략이 불가능합니다.")
                )));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("공개 여부가 Null이면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToCreatePlan_whenIsPublicNull() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플랜1", null);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("isPublic: 공개 여부를 지정해야 합니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("여행 시작일이 Null이면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenStartDateIsNull() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플랜1", true, null, LocalDate.parse("2025-09-30"));

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("startDate: 여행 시작일은 생략이 불가능합니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("여행 종료일이 Null이면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenEndDateIsNull() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("플랜1", true, LocalDate.parse("2025-09-30"), null);

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("endDate: 여행 종료일은 생략이 불가능합니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    @DisplayName("accessToken이 만료되었으면 플랜 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenAccessTokenExpired() throws Exception {
        //given
        String token = buildToken(userId, "access", issuer, Instant.now().minusSeconds(1));
        CreatePlanRequest request = getCreatePlanRequest("플랜1", true);

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."))
                .andReturn();
    }

    @DisplayName("accessToken이 아닐 경우 플랜 생성에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenAccessTokenIsMissingOrInvalid() throws Exception {
        //given
        String token = buildToken(userId, "refresh", issuer, Instant.now().plusSeconds(1000));
        CreatePlanRequest request = getCreatePlanRequest("플랜1", true);

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."))
                .andReturn();
    }

    @DisplayName("이미 탈퇴, 삭제, 정지, 차단된 유저는 플랜 생성에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("inactiveUserStates")
    void shouldReturn404_whenUserIsInactive(String role, UserStatus userStatus) throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(userStatus)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));
        CreatePlanRequest request = getCreatePlanRequest("플랜1", true);

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."))
                .andReturn();
    }

    private static Stream<Arguments> inactiveUserStates() {
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("차단된 유저", UserStatus.BLOCKED)
        );
    }

    @DisplayName("종료일이 시작일보다 앞서면 플랜 생성에 실패하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenEndDateIsBeforeStartDate() throws Exception {
        //given
        CreatePlanRequest request = getCreatePlanRequest("경주 여행", true, LocalDate.parse("2025-09-30"), LocalDate.parse("2025-09-01"));

        String token = buildToken(userId, "access", issuer, Instant.now().plusSeconds(30000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/plans")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("종료 날짜가 시작 날짜보다 앞설 수 없습니다."));

        assertThat(planRepository.count()).isZero();
        assertThat(planUsersRepository.count()).isZero();
    }

    private static CreatePlanRequest getCreatePlanRequest(String planName, Boolean isPublic, LocalDate startDate,
                                                          LocalDate endDate) {
        return CreatePlanRequest.builder()
                .name(planName)
                .isPublic(isPublic)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private static CreatePlanRequest getCreatePlanRequest(String planName, Boolean isPublic) {
        return getCreatePlanRequest(planName, isPublic, LocalDate.parse("2025-09-29"), LocalDate.parse("2025-09-30"));
    }

}
