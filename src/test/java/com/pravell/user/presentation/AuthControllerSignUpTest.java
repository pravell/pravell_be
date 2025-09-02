package com.pravell.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import com.pravell.user.presentation.request.SignUpRequest;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("회원가입 통합 테스트")
class AuthControllerSignUpTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("회원가입에 성공한다.")
    @Test
    void shouldSignUpSuccessfully() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isPresent();
        assertThat(user.get().getNickname()).isEqualTo(request.getNickname());
        assertThat(user.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @DisplayName("id가 비어있으면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenIdIsNull() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .password("testPassword")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 생략이 불가능합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("id가 공백이면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenIdIsBlank() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id(" ")
                .password("testPassword")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("id: 아이디는 생략이 불가능합니다."),
                        containsString("id: 아이디는 2 ~ 30자여야 합니다.")
                )));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("id가 2자 미만이면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenIdIsTooShort() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("i")
                .password("testPassword")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 2 ~ 30자여야 합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("id가 30자를 초과하면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenIdIsTooLong() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("d".repeat(31))
                .password("testPassword")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("id: 아이디는 2 ~ 30자여야 합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("password가 비어있으면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenPasswordIsNull() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 생략이 불가능합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("password가 공백이면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenPasswordIsBlank() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password(" ")
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("password: 비밀번호는 생략이 불가능합니다."),
                        containsString("password: 비밀번호는 8 ~ 64자여야 합니다.")
                )));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("password가 8자 미만이면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenPasswordIsTooShort() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("t".repeat(7))
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 8 ~ 64자여야 합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("password가 64자를 초과하면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenPasswordIsTooLong() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("t".repeat(65))
                .nickname("nickname")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("password: 비밀번호는 8 ~ 64자여야 합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("nickname이 비어있으면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenNicknameIsNull() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 생략이 불가능합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("nickname이 공백이면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenNicknameIsBlank() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname(" ")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("nickname: 닉네임은 생략이 불가능합니다."),
                        containsString("nickname: 닉네임은 2 ~ 30자여야 합니다.")
                )));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("nickname이 2자 미만이면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenNicknameIsTooShort() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("n")
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 2 ~ 30자여야 합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("nickname이 30자를 초과하면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenNicknameIsTooLong() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("n".repeat(31))
                .build();

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: 닉네임은 2 ~ 30자여야 합니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("이미 닉네임이 존재하면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenNicknameAlreadyExists() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("nickname")
                .build();

        userRepository.save(User.createUser("userId", "passwordTest", request.getNickname()).getUser());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 닉네임입니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isNotPresent();
    }

    @DisplayName("이미 아이디가 존재하면 회원가입에 실패한다.")
    @Test
    void shouldFailToSignUp_WhenIdAlreadyExists() throws Exception {
        //given
        SignUpRequest request = SignUpRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("nickname")
                .build();

        userRepository.save(User.createUser(request.getId(), "passwordTest", "testnickname").getUser());

        //when, then
        mockMvc.perform(
                        post("/api/v1/auth/sign-up")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Conflict"))
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다."));

        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isPresent();
        assertThat(user.get().getNickname()).isNotEqualTo(request.getNickname());
        assertThat(user.get().getPassword()).isNotEqualTo(request.getPassword());
    }

}
