package com.pravell.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pravell.common.exception.InvalidCredentialsException;
import com.pravell.user.application.dto.request.SignInApplicationRequest;
import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.domain.exception.UserNotFoundException;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.repository.RefreshTokenRepository;
import com.pravell.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AuthFacadeSignInTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @DisplayName("로그인에 성공한다.")
    @Test
    void shouldSignInSuccessfully() {
        //given
        SignInApplicationRequest request = getSignInApplicationRequest();

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        userRepository.save(User.createUser(request.getId(), encodedPassword, "testNickname").getUser());

        //when
        TokenResponse tokenResponse = authFacade.signIn(request);

        //then
        assertThat(tokenResponse.getAccessToken()).isNotBlank();
        assertThat(tokenResponse.getRefreshToken()).isNotBlank();
    }

    @DisplayName("로그인 한 유저를 찾을 수 없으면 예외가 발생한다.")
    @Test
    void tokenContainsValidInformation() {
        //given
        SignInApplicationRequest request = getSignInApplicationRequest();

        //when, then
        assertThatThrownBy(() -> authFacade.signIn(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저를 찾을 수 없습니다.");
    }

    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다.")
    @Test
    void failToLogin_whenPasswordIsIncorrect() {
        //given
        SignInApplicationRequest request = getSignInApplicationRequest();

        userRepository.save(User.createUser(request.getId(), "passwordd", "testNickname").getUser());

        //when, then
        assertThatThrownBy(() -> authFacade.signIn(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @DisplayName("로그인에 성공하면 RefreshToken과 AccessToken이 재발급된다.")
    @Test
    void shouldIssueNewTokens_whenLoginSucceeds() {
        //given
        SignUpApplicationRequest signUpApplicationRequest = SignUpApplicationRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("testNickname")
                .build();

        TokenResponse tokenResponse = authFacade.signUp(signUpApplicationRequest);

        SignInApplicationRequest request = SignInApplicationRequest.builder()
                .id(signUpApplicationRequest.getId())
                .password(signUpApplicationRequest.getPassword())
                .build();

        //when
        TokenResponse signInTokenResponse = authFacade.signIn(request);

        //then
        assertThat(tokenResponse.getAccessToken()).isNotEqualTo(signInTokenResponse.getAccessToken());
        assertThat(tokenResponse.getRefreshToken()).isNotEqualTo(signInTokenResponse.getRefreshToken());

        Optional<User> user = userRepository.findByUserId(signUpApplicationRequest.getId());
        String refreshToken = refreshTokenRepository.findByUserId(user.get().getId());
        assertThat(refreshToken).isEqualTo(signInTokenResponse.getRefreshToken());
        assertThat(refreshToken).isNotEqualTo(tokenResponse.getRefreshToken());
    }

    private SignInApplicationRequest getSignInApplicationRequest() {
        return SignInApplicationRequest.builder()
                .id("testId")
                .password("testPassword")
                .build();
    }

}
