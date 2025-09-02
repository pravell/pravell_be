package com.pravell.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.ControllerTestSupport;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.RefreshTokenRepository;
import com.pravell.user.domain.repository.UserRepository;
import com.pravell.user.presentation.request.SignInRequest;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("로그인 통합 테스트")
class AuthControllerSignInTest extends ControllerTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("로그인에 성공한다.")
    @Test
    void shouldSignInSuccessfully() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("testPassword")
                .build();

        String encodePassword = passwordEncoder.encode(request.getPassword());
        userRepository.save(User.createUser(request.getId(), encodePassword, "testUser").getUser());

        Optional<User> user = userRepository.findByUserId(request.getId());
        String oldRefreshToken = refreshTokenRepository.findByUserId(user.get().getId());

        //when, then
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.notNullValue()))
                .andReturn();

        String json = mvcResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(json, TokenResponse.class);

        String storedRefreshToken = refreshTokenRepository.findByUserId(user.get().getId());
        assertThat(storedRefreshToken).isNotEqualTo(oldRefreshToken);
        assertThat(storedRefreshToken).isEqualTo(tokenResponse.getRefreshToken());

        String header = mvcResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).contains("refreshToken=" + tokenResponse.getRefreshToken());
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("Secure");
        assertThat(header).contains("Path=/");
        assertThat(header).contains("SameSite=None");
        assertThat(header).contains("Max-Age=1209600");
    }

    @DisplayName("id가 비어있으면 로그인에 실패한다.")
    @Test
    void shouldFailToSignIn_WhenIdIsNull() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .password("testPassword")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 생략이 불가능합니다."));
    }

    @DisplayName("id가 공백이면 로그인에 실패한다.")
    @Test
    void shouldFailToSignIn_WhenIdIsBlank() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id(" ")
                .password("testPassword")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 생략이 불가능합니다."));
    }

    @DisplayName("password가 비어있으면 로그인에 실패한다.")
    @Test
    void shouldFailToSignIn_WhenPasswordIsNull() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 생략이 불가능합니다."));
    }

    @DisplayName("password가 공백이면 로그인에 실패한다.")
    @Test
    void shouldFailToSignIn_WhenPasswordIsBlank() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password(" ")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 생략이 불가능합니다."));
    }

    @DisplayName("해당 유저가 없으면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserDoesNotExist() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("password")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("비밀번호가 일치하지 않으면 401을 반환한다.")
    @Test
    void shouldReturn401_whenPasswordDoesNotMatch() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("password")
                .build();

        String encodePassword = passwordEncoder.encode("pppassworddd");
        userRepository.save(User.createUser(request.getId(), encodePassword, "testUser").getUser());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @DisplayName("해당 유저가 이미 탈퇴한 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsWithdrawn() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("password")
                .build();

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .userId(request.getId())
                .password(request.getPassword())
                .nickname("nickname")
                .status(UserStatus.WITHDRAWN)
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 삭제된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsDeleted() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("password")
                .build();

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .userId(request.getId())
                .password(request.getPassword())
                .nickname("nickname")
                .status(UserStatus.DELETED)
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 정지된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsSuspended() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("password")
                .build();

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .userId(request.getId())
                .password(request.getPassword())
                .nickname("nickname")
                .status(UserStatus.SUSPENDED)
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

    @DisplayName("해당 유저가 이미 차단된 유저라면 404를 반환한다.")
    @Test
    void shouldReturn404_whenUserIsBlocked() throws Exception {
        //given
        SignInRequest request = SignInRequest.builder()
                .id("testId")
                .password("password")
                .build();

        userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .userId(request.getId())
                .password(request.getPassword())
                .nickname("nickname")
                .status(UserStatus.BLOCKED)
                .build());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Not Found"))
                .andExpect(jsonPath("$.message").value("유저를 찾을 수 없습니다."));
    }

}
