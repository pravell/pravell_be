package com.pravell.plan.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.ControllerTestSupport;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.plan.presentation.request.UpdatePlanRequest;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlanControllerUpdateTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsersRepository planUsersRepository;

    private final User owner = User.builder()
            .id(UUID.randomUUID())
            .userId("testId")
            .password("passwordd")
            .nickname("nickname")
            .status(UserStatus.ACTIVE)
            .build();


    private final User member = User.builder()
            .id(UUID.randomUUID())
            .userId("testId11")
            .password("passwordd")
            .nickname("nickname11")
            .status(UserStatus.ACTIVE)
            .build();

    private final User unMember = User.builder()
            .id(UUID.randomUUID())
            .userId("testId111")
            .password("passwordd")
            .nickname("nickname111")
            .status(UserStatus.ACTIVE)
            .build();


    private final Plan publicPlan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isDeleted(false)
            .isPublic(true)
            .startDate(LocalDate.parse("2025-09-29"))
            .endDate(LocalDate.parse("2025-09-30"))
            .build();


    private final PlanUsers planOwner = PlanUsers.builder()
            .planId(publicPlan.getId())
            .userId(owner.getId())
            .planUserStatus(PlanUserStatus.OWNER)
            .build();


    private final PlanUsers planMember = PlanUsers.builder()
            .planId(publicPlan.getId())
            .userId(member.getId())
            .planUserStatus(PlanUserStatus.MEMBER)
            .build();

    private final Plan privatePlan = Plan.builder()
            .id(UUID.randomUUID())
            .name("경주 여행")
            .isDeleted(false)
            .isPublic(false)
            .startDate(LocalDate.parse("2025-09-29"))
            .endDate(LocalDate.parse("2025-09-30"))
            .build();


    private final PlanUsers planOwner2 = PlanUsers.builder()
            .planId(privatePlan.getId())
            .userId(owner.getId())
            .planUserStatus(PlanUserStatus.OWNER)
            .build();


    private final PlanUsers planMember2 = PlanUsers.builder()
            .planId(privatePlan.getId())
            .userId(member.getId())
            .planUserStatus(PlanUserStatus.MEMBER)
            .build();


    @BeforeEach
    void setUp() {
        userRepository.saveAll(List.of(owner, member, unMember));
        planRepository.saveAll(List.of(publicPlan, privatePlan));
        planUsersRepository.saveAll(List.of(planMember, planOwner, planOwner2, planMember2));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
        planRepository.deleteAllInBatch();
        planUsersRepository.deleteAllInBatch();
    }

    @DisplayName("해당 플랜의 OWNER일 경우, 플랜 이름 업데이트에 성공한다.")
    @Test
    void shouldUpdatePlanNameSuccessfully_whenUserIsOwner() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .name("변경 할 이름")
                .build();

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(publicPlan.getId().toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.isPublic").value(publicPlan.getIsPublic()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getName()).isNotEqualTo(publicPlan.getName());
        assertThat(afterPlan.get().getName()).isEqualTo(request.getName());
    }

    @DisplayName("해당 플랜의 OWNER일 경우, 플랜 공개 여부 업데이트에 성공한다.")
    @Test
    void shouldUpdatePlanVisibilitySuccessfully_whenUserIsOwner() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .build();

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(publicPlan.getId().toString()))
                .andExpect(jsonPath("$.name").value(publicPlan.getName()))
                .andExpect(jsonPath("$.isPublic").value(request.getIsPublic()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsPublic()).isNotEqualTo(publicPlan.getIsPublic());
        assertThat(afterPlan.get().getIsPublic()).isEqualTo(request.getIsPublic());
    }

    @DisplayName("해당 플랜의 OWNER일 경우, 플랜 공개 여부와 이름 업데이트에 성공한다.")
    @Test
    void shouldUpdatePlanNameAndVisibilitySuccessfully_whenUserIsOwner() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());


        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(publicPlan.getId().toString()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.isPublic").value(request.getIsPublic()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getIsPublic()).isNotEqualTo(publicPlan.getIsPublic());
        assertThat(afterPlan.get().getIsPublic()).isEqualTo(request.getIsPublic());
        assertThat(afterPlan.get().getName()).isNotEqualTo(publicPlan.getName());
        assertThat(afterPlan.get().getName()).isEqualTo(request.getName());
    }

    @DisplayName("해당 플랜의 MEMBER일 경우, 플랜 이름 업데이트에 실패하고, 403을 반환한다.")
    @Test
    void shouldThrowAccessDenied_whenMemberTriesToUpdatePlanName() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .name("변경 할 이름")
                .build();

        String token = buildToken(member.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 수정 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(afterPlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("해당 플랜의 MEMBER일 경우, 플랜 공개 여부 업데이트에 실패하고 403을 반환한다.")
    @Test
    void shouldThrowAccessDenied_whenMemberTriesToUpdatePlanVisibility() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .build();

        String token = buildToken(member.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 수정 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
    }

    @DisplayName("해당 플랜의 MEMBER일 경우, 플랜 공개 여부와 이름 업데이트에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403_whenMemberTriesToUpdatePlanNameAndVisibility() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(member.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 수정 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("해당 플랜에 참여하지 않은 유저의 경우, 플랜 이름 업데이트에 실패하고, 403을 반환한다.")
    @Test
    void shouldThrowAccessDenied_whenUnrelatedUserTriesToUpdatePlanName() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .name("변경 할 이름")
                .build();

        String token = buildToken(unMember.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 수정 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(afterPlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(afterPlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("해당 플랜에 참여하지 않은 유저의 경우, 플랜 공개 여부 업데이트에 실패하고 403을 반환한다.")
    @Test
    void shouldThrowAccessDenied_whenUnrelatedUserTriesToUpdatePlanVisibility() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .build();

        String token = buildToken(unMember.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 수정 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
    }

    @DisplayName("해당 플랜에 참여하지 않은 유저의 경우, 플랜 공개 여부와 이름 업데이트에 실패하고 403을 반환한다.")
    @Test
    void shouldReturn403_whenUnrelatedUserTriesToUpdatePlanNameAndVisibility() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(unMember.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("해당 리소스를 수정 할 권한이 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("업데이트 할 이름이 2자 미만이면, 업데이트에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenPlanNameIsTooShort() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("경")
                .build();

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 플랜 이름은 2 ~ 20자 사이여야 합니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("업데이트 할 이름이 20자 초과면, 업데이트에 실패하고 400을 반환한다.")
    @Test
    void shouldReturn400_whenPlanNameIsTooLong() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("경".repeat(21))
                .build();

        String token = buildToken(owner.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("name: 플랜 이름은 2 ~ 20자 사이여야 합니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("유저가 존재하지 않으면 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("탈퇴한 유저라면 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnUserTriesToUpdatePlan() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userIdd")
                .password("passwordd")
                .nickname("nicknema")
                .status(UserStatus.WITHDRAWN)
                .build();
        userRepository.save(user);

        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("삭제된 유저라면 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedUserTriesToUpdatePlan() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userIdd")
                .password("passwordd")
                .nickname("nicknema")
                .status(UserStatus.DELETED)
                .build();
        userRepository.save(user);

        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("정지된 유저라면 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedUserTriesToUpdatePlan() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userIdd")
                .password("passwordd")
                .nickname("nicknema")
                .status(UserStatus.SUSPENDED)
                .build();
        userRepository.save(user);

        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

    @DisplayName("차단된 유저라면 업데이트에 실패하고 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedUserTriesToUpdatePlan() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userIdd")
                .password("passwordd")
                .nickname("nicknema")
                .status(UserStatus.BLOCKED)
                .build();
        userRepository.save(user);

        UpdatePlanRequest request = UpdatePlanRequest.builder()
                .isPublic(false)
                .name("업데이트할이름")
                .build();

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(10000));

        Optional<Plan> beforePlan = planRepository.findById(publicPlan.getId());
        assertThat(beforePlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());

        //when, then
        mockMvc.perform(
                        patch("/api/v1/plans/" + publicPlan.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));

        Optional<Plan> afterPlan = planRepository.findById(publicPlan.getId());
        assertThat(afterPlan).isPresent();
        assertThat(beforePlan.get().getIsPublic()).isEqualTo(publicPlan.getIsPublic());
        assertThat(beforePlan.get().getIsPublic()).isNotEqualTo(request.getIsPublic());
        assertThat(beforePlan.get().getName()).isEqualTo(publicPlan.getName());
        assertThat(beforePlan.get().getName()).isNotEqualTo(request.getName());
    }

}
