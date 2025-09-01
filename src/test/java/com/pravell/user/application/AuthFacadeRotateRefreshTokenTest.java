package com.pravell.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pravell.common.exception.InvalidCredentialsException;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.repository.RefreshTokenRepository;
import com.pravell.user.domain.repository.UserRepository;
import com.pravell.user.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthFacadeRotateRefreshTokenTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenTtl;

    private final UUID id = UUID.randomUUID();
    private final String userId = "testId";
    private final String password = "testpassword";
    private String refreshToken;
    private Key key;

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        String encodePassword = passwordEncoder.encode(password);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        User user = User.builder()
                .id(id)
                .userId(userId)
                .password(encodePassword)
                .nickname("테스트유저")
                .build();

        refreshToken = jwtUtil.createRefreshToken(user);

        userRepository.save(user);

        refreshTokenRepository.save(id, refreshToken, refreshTokenTtl);
    }

    @DisplayName("refreshToken이 만료되지 않았고, 저장된 값하고 동일하면 AccessToken, RefreshToken 재발급에 성공한다.")
    @Test
    void reissueTokens_whenRefreshTokenIsValidAndMatchesStoredValue() {
        //when
        TokenResponse tokenResponse = authFacade.rotateRefreshAndIssueAccess(refreshToken);

        //then
        assertThat(refreshToken).isNotEqualTo(tokenResponse.getRefreshToken());

        String findRefreshToken = refreshTokenRepository.findByUserId(id);
        assertThat(findRefreshToken).isEqualTo(tokenResponse.getRefreshToken());
    }

    @DisplayName("토큰 Type이 Refresh가 아닐 경우 토큰 검증 시 예외가 발생한다.")
    @Test
    void failToValidateToken_whenTypeIsNotRefresh() {
        //given
        String token = buildToken("create", issuer, Instant.now().plusSeconds(60));

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(token))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("토큰이 올바르지 않습니다.");
    }

    @DisplayName("RefreshToken이 만료되면 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenExpired() {
        //given
        String token = buildToken("refresh", issuer, Instant.now().minusSeconds(10));

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(token))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("토큰이 올바르지 않습니다.");
    }

    @DisplayName("issuer가 일치하지 않으면 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenIssuerMismatch() {
        //given
        String refreshToken = buildToken("refresh", "another-issure", Instant.now().plusSeconds(60));

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("토큰이 올바르지 않습니다.");
    }

    @DisplayName("서명이 변조된 토큰은 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenTokenTampered() {
        //given
        User user = User.builder()
                .id(id)
                .userId(userId)
                .password(password)
                .nickname("테스트유저")
                .build();

        String refreshToken = jwtUtil.createRefreshToken(user);
        String tampered = refreshToken.substring(0, refreshToken.length() - 2) + "aa";

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(tampered))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("토큰이 올바르지 않습니다.");
    }

    @DisplayName("저장된 RefreshToken과 다를 경우, 검증에 실패한다.")
    @Test
    void failToValidateRefreshToken_whenTokenIsDifferentFromStored() {
        //given
        User user = User.builder()
                .id(id)
                .userId(userId)
                .password(password)
                .nickname("테스트유저")
                .build();

        String refreshToken = jwtUtil.createRefreshToken(user);

        //when, then
        assertThatThrownBy(() -> authFacade.rotateRefreshAndIssueAccess(refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("토큰이 올바르지 않습니다.");
    }

    private String buildToken(String typ, String iss, Instant exp) {
        return Jwts.builder()
                .subject(id.toString())
                .issuer(iss)
                .claim("typ", typ)
                .expiration(Date.from(exp))
                .issuedAt(new Date())
                .signWith(key)
                .compact();
    }

}
