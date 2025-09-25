package com.pravell.expense.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.presentation.request.CreateExpenseRequest;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
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

class ExpenseControllerCreateTest extends ExpenseControllerTestSupport {

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
        expenseRepository.deleteAllInBatch();
    }

    @DisplayName("플랜의 멤버이거나 소유자라면 지출 추가에 성공한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideAuthorizedPlanUserStatusesForExpenseCreation")
    void shouldSucceedToAddExpense_whenUserIsOwnerOrMember(String role, PlanUserStatus planUserStatus)
            throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        MvcResult result = mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        //then
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        URI uri = URI.create(location);
        String idStr = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
        UUID expenseId = UUID.fromString(idStr);

        Optional<Expense> after = expenseRepository.findById(expenseId);
        assertThat(after).isPresent();
        assertThat(after.get().getTitle()).isEqualTo(request.getTitle());
        assertThat(after.get().getAmount()).isEqualTo(request.getAmount());
        assertThat(after.get().getDescription()).isEqualTo(request.getDescription());
        assertThat(after.get().getCreatedBy()).isEqualTo(user.getId());
        assertThat(after.get().getSpentAt()).isEqualTo(request.getSpentAt());
        assertThat(after.get().getPaidByUserId()).isEqualTo(request.getPaidByUserId());
        assertThat(after.get().getPlanId()).isEqualTo(plan.getId());
        assertThat(after.get().isDeleted()).isFalse();
    }

    private static Stream<Arguments> provideAuthorizedPlanUserStatusesForExpenseCreation() {
        return Stream.of(
                Arguments.of("멤버", PlanUserStatus.MEMBER),
                Arguments.of("소유자", PlanUserStatus.OWNER)
        );
    }

    @DisplayName("플랜에서 탈퇴, 차단, 강퇴당한 멤버기어나 비참여자라면 지출 추가에 실패하고, 403을 반환한다.")
    @ParameterizedTest(name = "[{index}] 권한 : {0}")
    @MethodSource("provideUnauthorizedPlanUserStatusesForExpenseCreation")
    void shouldFailToAddExpense_whenUserIsNotAuthorized(String role, PlanUserStatus planUserStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        if (planUserStatus != null) {
            PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), planUserStatus);
            planUsersRepository.save(planUsers);
        }

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스에 접근 할 권한이 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    private static Stream<Arguments> provideUnauthorizedPlanUserStatusesForExpenseCreation() {
        return Stream.of(
                Arguments.of("탈퇴", PlanUserStatus.WITHDRAWN),
                Arguments.of("차단", PlanUserStatus.BLOCKED),
                Arguments.of("강퇴", PlanUserStatus.KICKED),
                Arguments.of("비참여자", null)
        );
    }

    @DisplayName("플랜이 존재하지 않으면 지출 추가에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToAddExpense_whenPlanDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + UUID.randomUUID() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("플랜이 삭제되었으면 지출 추가에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToAddExpense_whenPlanIsDeleted() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(true);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("플랜을 찾을 수 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("지출을 추가하려는 유저가 존재하지 않으면 지출 추가에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToAddExpense_whenUserDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("지출을 추가하려는 유저가 삭제, 차단, 정지당했거나 탈퇴했으면 지출 추가에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactiveUserStatusesForExpenseCreation")
    void shouldFailToAddExpense_whenUserIsInactiveOrBanned(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(userStatus);
        userRepository.save(user);

        User paidUser = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(paidUser.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    private static Stream<Arguments> provideInactiveUserStatusesForExpenseCreation() {
        return Stream.of(
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("차단당한 유저", UserStatus.BLOCKED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN)
        );
    }

    @DisplayName("지출한 유저가 삭제, 차단, 정지당했거나 탈퇴했으면 지출 추가에 실패하고, 404를 반환한다.")
    @ParameterizedTest(name = "[{index}] 상태 : {0}")
    @MethodSource("provideInactivePaidUserStatuses")
    void shouldFailToAddExpense_whenPaidUserIsInactiveOrBanned(String status, UserStatus userStatus) throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        User paidUser = getUser(userStatus);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(paidUser.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    private static Stream<Arguments> provideInactivePaidUserStatuses() {
        return Stream.of(
                Arguments.of("삭제된 유저", UserStatus.DELETED),
                Arguments.of("차단당한 유저", UserStatus.BLOCKED),
                Arguments.of("정지된 유저", UserStatus.SUSPENDED),
                Arguments.of("탈퇴한 유저", UserStatus.WITHDRAWN)
        );
    }

    @DisplayName("지출한 유저가 존재하지 않으면 지출 추가에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToAddExpense_whenPaidUserDoesNotExist() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(UUID.randomUUID())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("타이틀이 50자 초과면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenTitleExceedsMaxLength() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저".repeat(51))
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 타이틀은 50자까지 가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("타이틀이 null이면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenTitleIsNull() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 타이틀은 생략이 불가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("타이틀이 공백이면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenTitleIsBlank() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title(" ")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: 타이틀은 생략이 불가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("결제 금액이 null이면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenAmountIsNull() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁")
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("amount: 결제 금액은 생략이 불가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("결제한 유저가 null이면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenPaidByUserIdIsNull() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁")
                .amount(20000L)
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("paidByUserId: 결제한 유저는 생략이 불가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("결제 시각이 null이면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenSpentAtIsNull() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁")
                .amount(20000L)
                .paidByUserId(user.getId())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("spentAt: 결제 시각은 생략이 불가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("설명이 255자 초과면 지출 추가에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenDescriptionExceedsMaxLength() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어".repeat(256))
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: 설명은 255자까지 가능합니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

    @DisplayName("토큰이 만료되었으면 지출 추가에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailToAddExpense_whenTokenIsExpired() throws Exception {
        //given
        User user = getUser(UserStatus.ACTIVE);
        userRepository.save(user);

        Plan plan = getPlan(false);
        planRepository.save(plan);

        PlanUsers planUsers = getPlanUsers(plan.getId(), user.getId(), PlanUserStatus.OWNER);
        planUsersRepository.save(planUsers);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(10000));

        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("저녁 식사")
                .amount(20000L)
                .paidByUserId(user.getId())
                .spentAt(LocalDateTime.now())
                .description("어디어디 식당")
                .build();

        //when
        mockMvc.perform(
                        post("/api/v1/plans/" + plan.getId() + "/expenses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."));

        //then
        assertThat(expenseRepository.count()).isZero();
    }

}
