package com.pravell.expense.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.expense.domain.model.Expense;
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

class ExpenseControllerDeleteTest extends ExpenseControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 멤버, 소유자라면 지출 내역 삭제에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedPlanUserStatusesForExpenseDelete")
    void shouldDeleteExpense_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
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

        Optional<Expense> before = expenseRepository.findById(expense.getId());
        assertThat(before).isPresent();
        assertThat(before.get().isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();
        assertThat(after.get().isDeleted()).isTrue();
    }

    private static Stream<Arguments> provideAuthorizedPlanUserStatusesForExpenseDelete() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴, 차단, 강퇴당했거나 비참여 유저라면 지출 내역 삭제에 실패하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanUserStatusesForExpenseDelete")
    void shouldFailToDeleteExpense_whenUserIsNotParticipant(String role, PlanUserStatus planUserStatus)
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

        Optional<Expense> before = expenseRepository.findById(expense.getId());
        assertThat(before).isPresent();
        assertThat(before.get().isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();
        assertThat(after.get().isDeleted()).isFalse();
    }

    private static Stream<Arguments> provideUnauthorizedPlanUserStatusesForExpenseDelete() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("강퇴", PlanUserStatus.KICKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("지출 내역이 이미 삭제되었으면 404를 반환한다.")
    @Test
    void shouldFailToDeleteExpense_whenExpenseAlreadyDeleted() throws Exception {
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

        Optional<Expense> before = expenseRepository.findById(expense.getId());
        assertThat(before).isPresent();
        assertThat(before.get().isDeleted()).isTrue();

        //when
        mockMvc.perform(
                        delete("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("지출 내역을 찾을 수 없습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();
        assertThat(after.get().isDeleted()).isTrue();
    }

    @DisplayName("지출 내역이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldFailToDeleteExpense_whenExpenseDoesNotExist() throws Exception {
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
                        delete("/api/v1/expenses/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("지출 내역을 찾을 수 없습니다."));
    }

    @DisplayName("지출 삭제를 요청한 유저가 삭제, 탈퇴, 정지, 차단 당했으면 지출 삭제에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatusesForExpenseDeletion")
    void shouldFailToDeleteExpense_whenRequestingUserIsInactiveOrBanned(String status, UserStatus userStatus)
            throws Exception {
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

        Optional<Expense> before = expenseRepository.findById(expense.getId());
        assertThat(before).isPresent();
        assertThat(before.get().isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();
        assertThat(after.get().isDeleted()).isFalse();
    }

    private static Stream<Arguments> provideInactiveUserStatusesForExpenseDeletion() {
        return Stream.of(
                Arguments.of("탈퇴", UserStatus.WITHDRAWN),
                Arguments.of("차단", UserStatus.BLOCKED),
                Arguments.of("정지", UserStatus.SUSPENDED),
                Arguments.of("삭제", UserStatus.DELETED)
        );
    }

    @DisplayName("지출 삭제를 요청한 유저가 존재하지 않으면 지출 삭제에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToDeleteExpense_whenRequestingUserDoesNotExist() throws Exception {
        //given
        Plan plan = getPlan(false);
        planRepository.save(plan);

        Expense expense = getExpense(plan.getId(), UUID.randomUUID(), UUID.randomUUID());
        expenseRepository.save(expense);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Expense> before = expenseRepository.findById(expense.getId());
        assertThat(before).isPresent();
        assertThat(before.get().isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();
        assertThat(after.get().isDeleted()).isFalse();
    }

    @DisplayName("토큰이 만료되었으면 지출 삭제에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToDeleteExpense_whenTokenIsExpired() throws Exception {
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

        Optional<Expense> before = expenseRepository.findById(expense.getId());
        assertThat(before).isPresent();
        assertThat(before.get().isDeleted()).isFalse();

        //when
        mockMvc.perform(
                        delete("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();
        assertThat(after.get().isDeleted()).isFalse();
    }

}
