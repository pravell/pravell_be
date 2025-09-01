package com.pravell.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.repository.UserRepository;
import com.pravell.user.util.JwtUtil;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthFacadeSignUpTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.issuer}")
    private String issuer;

    @DisplayName("회원가입에 성공하면 비밀번호가 암호화 되어 저장된다.")
    @Test
    void shouldSignUpSuccessfully() {
        //given
        SignUpApplicationRequest request = getSignUpApplicationRequest();

        //when
        TokenResponse tokenResponse = authFacade.signUp(request);

        //then
        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isPresent();
        assertThat(new BCryptPasswordEncoder().matches(request.getPassword(), user.get().getPassword())).isTrue();
        assertThat(user.get().getPassword()).isNotEqualTo(request.getPassword());

        assertThat(tokenResponse.getAccessToken()).isNotBlank();
        assertThat(tokenResponse.getRefreshToken()).isNotBlank();
    }

    @DisplayName("회원가입 후 발급된 토큰이 올바른 정보를 담고있으며, 유효하다.")
    @Test
    void tokenContainsValidInformation() {
        //given
        SignUpApplicationRequest request = getSignUpApplicationRequest();

        //when
        TokenResponse tokenResponse = authFacade.signUp(request);

        //then
        Optional<User> user = userRepository.findByUserId(request.getId());
        assertThat(user).isPresent();

        Claims accessTokenClaims = jwtUtil.getClaims(tokenResponse.getAccessToken());
        assertThat(accessTokenClaims.getIssuer()).isEqualTo(issuer);
        assertThat(accessTokenClaims.get("typ", String.class)).isEqualTo("access");
        assertThat(accessTokenClaims.get("id", String.class)).isEqualTo(user.get().getId().toString());
        assertThat(accessTokenClaims.getExpiration()).isAfter(new Date());
        assertThat(jwtUtil.isValidRefreshToken(tokenResponse.getAccessToken())).isFalse();

        Claims refreshTokenClaims = jwtUtil.getClaims(tokenResponse.getRefreshToken());
        assertThat(refreshTokenClaims.getIssuer()).isEqualTo(issuer);
        assertThat(refreshTokenClaims.get("typ", String.class)).isEqualTo("refresh");
        assertThat(refreshTokenClaims.getExpiration()).isAfter(new Date());
        assertThat(jwtUtil.isValidRefreshToken(tokenResponse.getRefreshToken())).isTrue();
    }

    @DisplayName("아이디가 중복되면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserIdAlreadyExists() {
        //given
        SignUpApplicationRequest request = getSignUpApplicationRequest();

        userRepository.save(User.createUser(request.getId(), "passwordTest", "nicknameTest").getUser());

        //when, then
        assertThatThrownBy(() -> authFacade.signUp(request))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @DisplayName("닉네임이 중복되면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenNicknameAlreadyExists() {
        //given
        SignUpApplicationRequest request = getSignUpApplicationRequest();

        userRepository.save(User.createUser("idTest", "passwordTest", request.getNickname()).getUser());

        //when, then
        assertThatThrownBy(() -> authFacade.signUp(request))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");
    }

    private SignUpApplicationRequest getSignUpApplicationRequest() {
        return SignUpApplicationRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("nickname")
                .build();
    }

}
