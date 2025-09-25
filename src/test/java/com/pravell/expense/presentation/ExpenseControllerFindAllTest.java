package com.pravell.expense.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.pravell.expense.application.dto.response.ExpenseResponse;
import com.pravell.expense.domain.model.Expense;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
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

class ExpenseControllerFindAllTest extends ExpenseControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 멤버, 소유자는 지출 내역들을 확인할 수 있다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedPlanUserStatusesForExpenseView")
    void shouldSucceedToViewExpenses_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), user.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        objectMapper.findAndRegisterModules();
        List<ExpenseResponse> expenses = objectMapper.readValue(json, new TypeReference<List<ExpenseResponse>>() {
        });

        assertThat(expenses).hasSize(2)
                .extracting("title", "description", "paidByUserId")
                .containsExactlyInAnyOrder(
                        tuple(expense1.getTitle(), expense1.getDescription(), expense1.getPaidByUserId()),
                        tuple(expense2.getTitle(), expense2.getDescription(), expense2.getPaidByUserId())
                );
    }

    private static Stream<Arguments> provideAuthorizedPlanUserStatusesForExpenseView() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴, 차단, 강퇴당했거나 비참여 유저는 플랜의 지출 목록을 확인하지 못하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanUserStatusesForExpenseView")
    void shouldFailToViewExpenses_whenUserIsNotParticipantOrBanned(String role, PlanUserStatus planUserStatus)
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

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), user.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));
    }

    private static Stream<Arguments> provideUnauthorizedPlanUserStatusesForExpenseView() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("강퇴", PlanUserStatus.KICKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("특정 유저가 지출한 소비 내역만 확인할 수 있다.")
    @Test
    void shouldReturnOnlyExpensesCreatedBySpecifiedUser() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2));

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), paidUser.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .param("paidUserId", paidUser.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        objectMapper.findAndRegisterModules();
        List<ExpenseResponse> expenses = objectMapper.readValue(json, new TypeReference<List<ExpenseResponse>>() {
        });

        assertThat(expenses).hasSize(1)
                .extracting("title", "description", "paidByUserId")
                .containsExactlyInAnyOrder(
                        tuple(expense2.getTitle(), expense2.getDescription(), expense2.getPaidByUserId())
                );
    }

    @DisplayName("특정 날짜의 지출한 소비 내역만 확인할 수 있다.")
    @Test
    void shouldReturnOnlyExpensesCreatedOnSpecifiedDate() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2));

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId(),
                LocalDateTime.of(2025, 9, 8, 11, 12));
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), paidUser.getId(),
                LocalDateTime.of(2025, 9, 9, 11, 12));
        Expense expense3 = getExpense("title23", "description3", plan.getId(), user.getId(), paidUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12));

        Expense expense4 = getExpense("title4", "description4", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3, expense4));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .param("from", LocalDateTime.of(2025, 9, 8, 11, 11).toString())
                                .param("to", LocalDateTime.of(2025, 9, 9, 11, 13).toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        objectMapper.findAndRegisterModules();
        List<ExpenseResponse> expenses = objectMapper.readValue(json, new TypeReference<List<ExpenseResponse>>() {
        });

        assertThat(expenses).hasSize(2)
                .extracting("title", "description", "paidByUserId")
                .containsExactlyInAnyOrder(
                        tuple(expense1.getTitle(), expense1.getDescription(), expense1.getPaidByUserId()),
                        tuple(expense2.getTitle(), expense2.getDescription(), expense2.getPaidByUserId())
                );
    }

    @DisplayName("종료 날짜가 시작 날짜보다 앞설 수 없다.")
    @Test
    void shouldFailToAddExpense_whenEndDateIsBeforeStartDate() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2));

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId(),
                LocalDateTime.of(2025, 9, 8, 11, 12));
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), paidUser.getId(),
                LocalDateTime.of(2025, 9, 9, 11, 12));
        Expense expense3 = getExpense("title23", "description3", plan.getId(), user.getId(), paidUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12));

        Expense expense4 = getExpense("title4", "description4", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3, expense4));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .param("to", LocalDateTime.of(2025, 9, 8, 11, 11).toString())
                                .param("from", LocalDateTime.of(2025, 9, 9, 11, 13).toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("종료 날짜가 시작 날짜보다 앞설 수 없습니다."));
    }

    @DisplayName("지출한 유저가 더이상 플랜에 속해있지 않으면 플랜에 속하지 않은 유저로 닉네임을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("providePlanUserStatusesForNicknameFallback")
    void shouldReturnExpensesWithPlaceholderNickname_whenPaidUserIsNotInPlan(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);
        if (planUserStatus != null) {
            PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidUser.getId(), planUserStatus);
            planUsersRepository.save(planUsers2);
        }

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), paidUser.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        objectMapper.findAndRegisterModules();
        List<ExpenseResponse> expenses = objectMapper.readValue(json, new TypeReference<List<ExpenseResponse>>() {
        });

        assertThat(expenses).hasSize(2)
                .extracting("title", "description", "paidByUserNickname")
                .containsExactlyInAnyOrder(
                        tuple(expense1.getTitle(), expense1.getDescription(), user.getNickname()),
                        tuple(expense2.getTitle(), expense2.getDescription(), "플랜에 속하지 않는 유저")
                );
    }

    private static Stream<Arguments> providePlanUserStatusesForNicknameFallback() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("강퇴", PlanUserStatus.KICKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("지출한 유저가 탈퇴, 차단, 강퇴, 정지당했으면 플랜에 속하지 않은 유저로 닉네임을 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInvalidUserStatusesForPaidUser")
    void shouldReturnExpensesWithPlaceholderNickname_whenPaidUserAccountIsInvalid(String role, UserStatus userStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidUser = getUser(userStatus);
        userRepository.saveAll(List.of(user, paidUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2));

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), paidUser.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        //then
        String json = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        objectMapper.findAndRegisterModules();
        List<ExpenseResponse> expenses = objectMapper.readValue(json, new TypeReference<List<ExpenseResponse>>() {
        });

        assertThat(expenses).hasSize(2)
                .extracting("title", "description", "paidByUserNickname")
                .containsExactlyInAnyOrder(
                        tuple(expense1.getTitle(), expense1.getDescription(), user.getNickname()),
                        tuple(expense2.getTitle(), expense2.getDescription(), "플랜에 속하지 않는 유저")
                );
    }

    private static Stream<Arguments> provideInvalidUserStatusesForPaidUser() {
        return Stream.of(
                Arguments.of("탈퇴", UserStatus.WITHDRAWN),
                Arguments.of("삭제", UserStatus.DELETED),
                Arguments.of("차단", UserStatus.BLOCKED),
                Arguments.of("정지", UserStatus.SUSPENDED)
        );
    }

    @DisplayName("플랜이 삭제되었으면 지출 내역을 확인하지 못하고 404를 반환한다.")
    @Test
    void shouldFailToViewExpenses_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), user.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("플랜이 존재하지 않으면 지출 내역을 확인하지 못하고 404를 반환한다.")
    @Test
    void shouldFailToViewExpenses_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + UUID.randomUUID() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("조회를 요청한 유저가 존재하지 않으면 지출 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToViewExpenses_whenRequestingUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("조회를 요청한 유저가 탈퇴, 차단, 삭제, 정지되었으면 지출 조회에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatusesForExpenseView")
    void shouldFailToViewExpenses_whenRequestingUserIsInactive(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), user.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));


        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideInactiveUserStatusesForExpenseView(){
        return Stream.of(
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN),
                Arguments.of("차단당한 유저", UserStatus.BLOCKED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("삭제된 유저", UserStatus.DELETED)
        );
    }

    @DisplayName("토큰이 만료되었으면 지출 조회에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToViewExpenses_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense1 = getExpense("title1", "description1", plan.getId(), user.getId(), user.getId());
        Expense expense2 = getExpense("title2", "description2", plan.getId(), user.getId(), user.getId());

        Expense expense3 = getExpense("title3", "description3", UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.saveAll(List.of(expense1, expense2, expense3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));


        //when, then
        mockMvc.perform(
                        get("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

}
