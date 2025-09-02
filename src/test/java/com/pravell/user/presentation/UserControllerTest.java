package com.pravell.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret-key}")
    private String secretKey;

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

    private String buildToken(UUID userId, String typ, String iss, Instant exp) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(iss)
                .claim("userId", userId.toString())
                .claim("typ", typ)
                .expiration(Date.from(exp))
                .issuedAt(new Date())
                .signWith(key, SIG.HS256)
                .compact();
    }

}
