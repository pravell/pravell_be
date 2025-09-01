package com.pravell.user.application;

import com.pravell.user.application.dto.request.SignInApplicationRequest;
import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.domain.event.UserCreatedEvent;
import com.pravell.user.domain.model.User;
import com.pravell.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthFacade {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public TokenResponse signUp(SignUpApplicationRequest signUpApplicationRequest) {
        log.info("회원가입을 진행합니다. id : {}", signUpApplicationRequest.getId());

        UserCreatedEvent userCreatedEvent = userService.persistUser(signUpApplicationRequest);

        String accessToken = jwtUtil.createAccessToken(userCreatedEvent.getUser());
        String refreshToken = jwtUtil.createRefreshToken(userCreatedEvent.getUser());

        refreshTokenService.saveRefreshToken(userCreatedEvent.getUser().getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse signIn(SignInApplicationRequest signInApplicationRequest) {
        User user = userService.findUser(signInApplicationRequest.getId());
        userService.verifyPassword(signInApplicationRequest.getPassword(), user.getPassword());

        String newRefreshToken = jwtUtil.createRefreshToken(user);
        refreshTokenService.updateToken(user.getId(), newRefreshToken);

        String newAccessToken = jwtUtil.createAccessToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

}
