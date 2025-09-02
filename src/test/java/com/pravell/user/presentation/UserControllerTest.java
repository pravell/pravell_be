package com.pravell.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.ControllerTestSupport;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import com.pravell.user.presentation.request.UpdateUserRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class UserControllerTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("로그인한 유저의 프로필 정보를 조회한다.")
    @Test
    void shouldRetrieveLoggedInUserProfile() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(30000));

        //when, /then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.nickname").value(user.getNickname()))
                .andExpect(jsonPath("$.status").value(UserStatus.ACTIVE.name()))
                .andReturn();
    }

    @DisplayName("accessToken에 해당하는 사용자 정보가 없으면 프로필 정보 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenAccessTokenUserDoesNotExist() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."))
                .andReturn();
    }

    @DisplayName("accessToken이 만료되었으면 프로필 정보 조회에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenAccessTokenExpired() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(1));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."))
                .andReturn();
    }

    @DisplayName("accessToken이 아닐 경우 프로필 정보 조회에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturnUnauthorized_whenAccessTokenIsMissingOrInvalid() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().minusSeconds(1));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."))
                .andReturn();
    }

    @DisplayName("이미 탈퇴한 유저는 프로필 정보 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenUserHasWithdrawn() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.WITHDRAWN)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."))
                .andReturn();
    }

    @DisplayName("이미 삭제된 유저는 프로필 정보 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenUserHasDeleted() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.DELETED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."))
                .andReturn();
    }

    @DisplayName("이미 정지된 유저는 프로필 정보 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenUserHasSuspended() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.SUSPENDED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."))
                .andReturn();
    }

    @DisplayName("이미 차단된 유저는 프로필 정보 조회에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturnNotFound_whenUserHasBlocked() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(100000));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."))
                .andReturn();
    }

    @DisplayName("유저 탈퇴에 성공한다.")
    @Test
    void shouldWithdrawUserSuccessfully() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNoContent());

        //then
        Optional<User> afterUser = userRepository.findById(user.getId());
        assertThat(afterUser).isPresent();
        assertThat(afterUser.get().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @DisplayName("탈퇴시 토큰에 해당하는 유저가 존재하지 않을 경우 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExistForGivenToken() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("accessToken이 만료되었으면 탈퇴에 실패하고, 401을 반환한다.")
    @Test
    void shouldReturn401Unauthorized_whenAccessTokenIsExpired() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().minusSeconds(1));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."))
                .andReturn();
    }

    @DisplayName("accessToken이 아닐 경우 탈퇴에 실패하고, 401을 반환한다.")
    @Test
    void shouldFailWithdrawal_whenAccessTokenInvalid() throws Exception {
        //given
        User user = User.createUser("userId", "passwordTest", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "refresh", issuer, Instant.now().minusSeconds(1));

        //when, then
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("토큰이 올바르지 않습니다."))
                .andReturn();
    }

    @DisplayName("이미 탈퇴된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenWithdrawnUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.WITHDRAWN)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 삭제된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenDeletedUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.DELETED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 정지된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenSuspendedUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.SUSPENDED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("이미 차단된 유저는 탈퇴에 실패하고, 404를 반환한다.")
    @Test
    void shouldReturn404_whenBlockedUserTriesToWithdrawAgain() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        //when, then
        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("유저 정보가 성공적으로 업데이트된다.")
    @Test
    void shouldUpdateUserInfoSuccessfully() throws Exception {
        //given
        User user = User.createUser("userId", "passwordd", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.nickname").value(request.getNickname()))
                .andExpect(jsonPath("$.status").value(UserStatus.ACTIVE.name()));

        Optional<User> afterUser = userRepository.findById(user.getId());
        assertThat(afterUser).isPresent();
        assertThat(afterUser.get().getNickname()).isEqualTo(request.getNickname());
    }

    @DisplayName("업데이트 할 닉네임이 2자 미만이라면 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenLengthIsLessThanTwo() throws Exception {
        //given
        User user = User.createUser("userId", "passwordd", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 2 ~ 30자여야 합니다."));

        Optional<User> afterUser = userRepository.findById(user.getId());
        assertThat(afterUser).isPresent();
        assertThat(afterUser.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(afterUser.get().getNickname()).isEqualTo(user.getNickname());
    }

    @DisplayName("업데이트 할 닉네임이 30자 초과라면 업데이트에 실패하고, 400을 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenLengthExceedsLimit() throws Exception {
        //given
        User user = User.createUser("userId", "passwordd", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업".repeat(31))
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 2 ~ 30자여야 합니다."));

        Optional<User> afterUser = userRepository.findById(user.getId());
        assertThat(afterUser).isPresent();
        assertThat(afterUser.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(afterUser.get().getNickname()).isEqualTo(user.getNickname());
    }

    @DisplayName("해당 닉네임이 이미 존재하면 업데이트에 실패하고, 409를 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenNicknameAlreadyExists() throws Exception {
        //given
        User nicknameAlreadyExists = User.createUser("user", "passwordd", "already").getUser();
        userRepository.save(nicknameAlreadyExists);

        User user = User.createUser("userId", "passwordd", "nickname").getUser();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname(nicknameAlreadyExists.getNickname())
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 닉네임입니다."));

        Optional<User> afterUser = userRepository.findById(user.getId());
        assertThat(afterUser).isPresent();
        assertThat(afterUser.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(afterUser.get().getNickname()).isEqualTo(user.getNickname());
    }

    @DisplayName("해당 유저가 존재하지 않으면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenUserNotFound() throws Exception {
        //given
        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 탈퇴한 유저라면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenUserIsWithdrawn() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.WITHDRAWN)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 삭제된 유저라면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenUserIsDeleted() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.DELETED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 정지된 유저라면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenUserIsSuspended() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.SUSPENDED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 차단된 유저라면 업데이트에 실패하고, 404를 반환한다.")
    @Test
    void shouldFailToUpdateNickname_whenUserIsBlocked() throws Exception {
        //given
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId("userId")
                .password("passwordd")
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build();
        userRepository.save(user);

        String token = buildToken(user.getId(), "access", issuer, Instant.now().plusSeconds(1000));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .nickname("업데이트 할 닉네임")
                .build();

        //when, then
        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

}
