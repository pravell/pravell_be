package com.pravell.expense.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.presentation.request.UpdateExpenseRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

class ExpenseControllerUpdateTest extends ExpenseControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 멤버, 소유자라면 지출 내역 업데이트에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedPlanUserStatusesForExpenseUpdate")
    void shouldUpdateExpense_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12));
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(request.getTitle()))
                .andExpect(jsonPath("$.amount").value(request.getAmount()))
                .andExpect(jsonPath("$.paidByUserId").value(request.getPaidByUserId().toString()))
                .andExpect(jsonPath("$.spentAt")
                        .value(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(request.getSpentAt())))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.paidByUserNickname").value(updateUser.getNickname()));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isNotEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isNotEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isNotEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isNotEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isNotEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideAuthorizedPlanUserStatusesForExpenseUpdate() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴, 차단, 강퇴당했거나 비참여 유저는 지출 내역을 수정하지 못하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanUserStatusesForExpenseUpdate")
    void shouldFailToUpdateExpense_whenUserIsNotParticipant(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12));
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isNotEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isNotEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isNotEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isNotEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("플랜에서 탈퇴, 차단, 강퇴당했거나 비참여 유저로 지출한 유저를 등록하면 지출 내역을 수정하지 못하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanUserStatusesForExpenseUpdate")
    void shouldFailToUpdateExpense_whenPaidUserIsNotParticipant(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers2, planUsers));
        if (planUserStatus != null) {
            PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), planUserStatus);
            planUsersRepository.save(planUsers3);
        }

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12));
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 유저는 플랜에 속해있지 않아 지출한 유저로 등록이 불가능합니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isNotEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isNotEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isNotEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isNotEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    private static Stream<Arguments> provideUnauthorizedPlanUserStatusesForExpenseUpdate() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("강퇴", PlanUserStatus.KICKED),
                Arguments.of("비참여", null)
        );
    }

    @DisplayName("지출 내역이 삭제되었으면 지출 내역 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateExpense_whenExpenseIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12), true);
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("지출 내역을 찾을 수 없습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isNotEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isNotEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isNotEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isNotEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("지출 내역이 존재하지 않으면 404를 반환한다.")
    @Test
    void shouldFailToUpdateExpense_whenExpenseDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/expenses/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("지출 내역을 찾을 수 없습니다."));
    }

    @DisplayName("수정할 타이틀이 50자 초과면 지출 내역 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToUpdateExpense_whenTitleExceedsMaxLength() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12), false);
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수".repeat(51))
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 타이틀은 50자까지 가능합니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isNotEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isNotEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isNotEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isNotEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("수정할 설명이 255자 초과면 지출 내역 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToUpdateExpense_whenDescriptionExceedsMaxLength() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12), false);
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수".repeat(256))
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: 설명은 255자까지 가능합니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isNotEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isNotEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isNotEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isNotEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

    @DisplayName("수정하고 싶은 내역만 수정이 가능하다.")
    @Test
    void shouldUpdateOnlySpecifiedFields() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12), false);
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(request.getTitle()))
                .andExpect(jsonPath("$.amount").value(request.getAmount()))
                .andExpect(jsonPath("$.paidByUserId").value(expense.getPaidByUserId().toString()))
                .andExpect(jsonPath("$.spentAt")
                        .value(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(request.getSpentAt())))
                .andExpect(jsonPath("$.description").value(expense.getDescription()))
                .andExpect(jsonPath("$.paidByUserNickname").value(paidByUser.getNickname()));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isNotEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isNotEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isNotEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
    }

    @DisplayName("토큰이 만료되었으면 지출 내역 업데이트에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToUpdateExpense_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        User paidByUser = getUser(UserStatus.ACTIVE);
        User updateUser = getUser(UserStatus.ACTIVE);
        userRepository.saveAll(List.of(user, paidByUser, updateUser));

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers2 = getPlanUsers(plan.getId(), paidByUser.getId(), PlanUserStatus.MEMBER);
        PlanUsers planUsers3 = getPlanUsers(plan.getId(), updateUser.getId(), PlanUserStatus.MEMBER);
        planUsersRepository.saveAll(List.of(planUsers, planUsers2, planUsers3));

        Expense expense = getExpense("저녁 식사", "어디어디 맛집", plan.getId(), user.getId(), paidByUser.getId(),
                LocalDateTime.of(2025, 9, 10, 11, 12), false);
        expenseRepository.save(expense);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("수정할 타이틀")
                .amount(1000000000L)
                .paidByUserId(updateUser.getId())
                .spentAt(LocalDateTime.of(2025, 9, 24, 11, 12))
                .description("수정할 설명")
                .build();

        //when
        mockMvc.perform(
                        patch("/api/v1/expenses/" + expense.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        Optional<Expense> after = expenseRepository.findById(expense.getId());
        assertThat(after).isPresent();

        assertThat(after.get().getTitle()).isEqualTo(expense.getTitle());
        assertThat(after.get().getTitle()).isNotEqualTo(request.getTitle());

        assertThat(after.get().getAmount()).isEqualTo(expense.getAmount());
        assertThat(after.get().getAmount()).isNotEqualTo(request.getAmount());

        assertThat(after.get().getPaidByUserId()).isEqualTo(expense.getPaidByUserId());
        assertThat(after.get().getPaidByUserId()).isNotEqualTo(request.getPaidByUserId());

        assertThat(after.get().getSpentAt()).isEqualTo(expense.getSpentAt());
        assertThat(after.get().getSpentAt()).isNotEqualTo(request.getSpentAt());

        assertThat(after.get().getDescription()).isEqualTo(expense.getDescription());
        assertThat(after.get().getDescription()).isNotEqualTo(request.getDescription());
    }

}
