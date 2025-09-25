package com.pravell.expense.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.expense.domain.model.Expense;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ExpenseControllerFindTest extends ExpenseControllerTestSupport {

    @DisplayName("플랜의 멤버, 소유자는 지출 내역을 조회할 수 있다.")
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

        Expense expense = getExpense(plan.getId(), user.getId(), user.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(expense.getTitle()))
                .andExpect(jsonPath("$.amount").value(expense.getAmount()))
                .andExpect(jsonPath("$.paidByUserId").value(expense.getPaidByUserId().toString()))
                .andExpect(jsonPath("$.spentAt")
                        .value(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(expense.getSpentAt())))
                .andExpect(jsonPath("$.description").value(expense.getDescription()))
                .andExpect(jsonPath("$.paidByUserNickname").value(user.getNickname()));
    }

    private static Stream<Arguments> provideAuthorizedPlanUserStatusesForExpenseView() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴, 차단, 강퇴당했거나 비참여 유저는 지출 내역을 확인하지 못하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanUserStatusesForExpenseView")
    void shouldFailToViewExpenses_whenUserIsNotParticipant(String role, PlanUserStatus planUserStatus)
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

        Expense expense = getExpense(plan.getId(), user.getId(), user.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
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

    @DisplayName("지출한 유저가 더이상 플랜에 속해있지 않으면 유저 닉네임을 플랜에 속하지 않는 유저로 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactivePlanUserStatusesForNicknameFallback")
    void shouldDisplayFallbackNickname_whenPaidUserIsNotInPlan(String status, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);
        if (planUserStatus != null) {
            PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), planUserStatus);
            planUsersRepository.save(planUsers2);
        }

        Expense expense = getExpense(plan.getId(), user.getId(), paidByUser.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(expense.getTitle()))
                .andExpect(jsonPath("$.amount").value(expense.getAmount()))
                .andExpect(jsonPath("$.paidByUserId").value(expense.getPaidByUserId().toString()))
                .andExpect(jsonPath("$.spentAt")
                        .value(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(expense.getSpentAt())))
                .andExpect(jsonPath("$.description").value(expense.getDescription()))
                .andExpect(jsonPath("$.paidByUserNickname").value("플랜에 속하지 않는 유저"));
    }

    private static Stream<Arguments> provideInactivePlanUserStatusesForNicknameFallback() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("강퇴", PlanUserStatus.KICKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("지출한 유저가 탈퇴, 정지, 차단, 삭제된 상태라면 유저 닉네임을 플랜에 속하지 않는 유저로 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatusesForNicknameFallback")
    void shouldDisplayFallbackNickname_whenPaidUserIsDeactivated(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(userStatus);
        userRepository.saveAll(List.of(user, paidByUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2));

        Expense expense = getExpense(plan.getId(), user.getId(), paidByUser.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(expense.getTitle()))
                .andExpect(jsonPath("$.amount").value(expense.getAmount()))
                .andExpect(jsonPath("$.paidByUserId").value(expense.getPaidByUserId().toString()))
                .andExpect(jsonPath("$.spentAt")
                        .value(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(expense.getSpentAt())))
                .andExpect(jsonPath("$.description").value(expense.getDescription()))
                .andExpect(jsonPath("$.paidByUserNickname").value("플랜에 속하지 않는 유저"));
    }

    private static Stream<Arguments> provideInactiveUserStatusesForNicknameFallback() {
        return Stream.of(
                Arguments.of("탈퇴", UserStatus.WITHDRAWN),
                Arguments.of("차단", UserStatus.BLOCKED),
                Arguments.of("정지", UserStatus.SUSPENDED),
                Arguments.of("삭제", UserStatus.DELETED)
        );
    }

    @DisplayName("지출 내역이 삭제되었으면 지출 내역 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToViewExpense_whenExpenseIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense = getExpense(plan.getId(), user.getId(), user.getId(), true);
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("지출 내역을 찾을 수 없습니다."));
    }

    @DisplayName("지출 내역이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldFailToViewExpense_whenExpenseDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("지출 내역을 찾을 수 없습니다."));
    }

    @DisplayName("지출 내역이 속한 플랜이 삭제되었으면 지출 내역 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToViewExpense_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense = getExpense(plan.getId(), user.getId(), user.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("지출 내역이 속한 플랜이 존재하지 않으면 지출 내역 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToViewExpense_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Expense expense = getExpense(UUID.randomUUID(), user.getId(), user.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));
    }

    @DisplayName("지출 내역 조회를 요청한 유저가 탈퇴, 정지, 차단, 삭제되었으면 지출 내역 조회에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatusesForExpenseView")
    void shouldFailToViewExpense_whenUserIsWithdrawnOrBanned(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense = getExpense(plan.getId(), user.getId(), user.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    private static Stream<Arguments> provideInactiveUserStatusesForExpenseView() {
        return Stream.of(
                Arguments.of("탈퇴", UserStatus.WITHDRAWN),
                Arguments.of("정지", UserStatus.SUSPENDED),
                Arguments.of("차단", UserStatus.BLOCKED),
                Arguments.of("삭제", UserStatus.DELETED)
        );
    }

    @DisplayName("지출 내역 조회를 요청한 유저가 존재하지 않으면 지출 내역 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToViewExpense_whenUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Expense expense = getExpense(plan.getId(), UUID.randomUUID(), UUID.randomUUID());
        expenseRepository.save(expense);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("유저의 토큰이 만료되었으면 지출 조회에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToViewExpense_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.save(planUsers);

        Expense expense = getExpense(plan.getId(), user.getId(), user.getId());
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));
    }

}
