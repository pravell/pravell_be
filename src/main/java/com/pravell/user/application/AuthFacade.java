package com.pravell.user.application;

import com.pravell.common.exception.InvalidCredentialsException;
import com.pravell.common.util.CommonJwtUtil;
import com.pravell.user.application.dto.request.SignInApplicationRequest;
import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.domain.event.UserCreatedEvent;
import com.pravell.user.domain.model.User;
import com.pravell.user.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
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
    private final CommonJwtUtil commonJwtUtil;

    public TokenResponse signUp(SignUpApplicationRequest signUpApplicationRequest) {
        log.info("회원가입을 진행합니다. id : {}", signUpApplicationRequest.getId());

        UserCreatedEvent userCreatedEvent = userService.persistUser(signUpApplicationRequest);

        String accessToken = jwtUtil.createAccessToken(userCreatedEvent.getUser());
        String refreshToken = jwtUtil.createRefreshToken(userCreatedEvent.getUser());

        refreshTokenService.saveRefreshToken(userCreatedEvent.getUser().getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse signIn(SignInApplicationRequest signInApplicationRequest) {
        User user = userService.findUserByUserId(signInApplicationRequest.getId());
        userService.verifyPassword(signInApplicationRequest.getPassword(), user.getPassword());

        String newRefreshToken = jwtUtil.createRefreshToken(user);
        refreshTokenService.updateToken(user.getId(), newRefreshToken);

        String newAccessToken = jwtUtil.createAccessToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void signOut(UUID userId, String refreshToken) {
        if (!commonJwtUtil.isValidRefreshToken(refreshToken)) {
            log.info("Refresh Token이 유효하지 않습니다. UserId : {}, RefreshToken : {}", userId, refreshToken);
            throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
        }

        Claims claims = commonJwtUtil.getClaims(refreshToken);
        if (!UUID.fromString(claims.getSubject()).equals(userId)) {
            log.info("올바르지 않은 Refresh Token입니다. UserId : {}, RefreshToken : {}", userId, refreshToken);
            throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
        }

        String storedRefreshToken = refreshTokenService.findRefreshToken(userId);
        if (storedRefreshToken == null) {
            return;
        }
        if (!storedRefreshToken.equals(refreshToken)) {
            log.info("세션 정보가 일치하지 않습니다. UserId : {}, RefreshToken : {}", userId, refreshToken);
            throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
        }

        refreshTokenService.revoke(userId);
    }

    public TokenResponse rotateRefreshAndIssueAccess(String refreshToken) {
        try {
            if (!commonJwtUtil.isValidRefreshToken(refreshToken)) {
                log.info("Refresh Token이 만료되었습니다. RefreshToken : {}", refreshToken);
                throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
            }

            Claims claims = jwtUtil.getClaims(refreshToken);
            String userId = claims.getSubject();
            User user = userService.findUserById(UUID.fromString(userId));

            String findRefreshToken = refreshTokenService.findRefreshToken(user.getId());
            if (!refreshToken.equals(findRefreshToken)) {
                log.info("Refresh Token이 일치하지 않습니다. RefreshToken : {}", refreshToken);
                throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
            }

            String newRefreshToken = jwtUtil.createRefreshToken(user);
            refreshTokenService.updateToken(user.getId(), newRefreshToken);

            String newAccessToken = jwtUtil.createAccessToken(user);

            return new TokenResponse(newAccessToken, newRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            log.info("Refresh Token 파싱/검증에 실패했습니다. RefreshToken : {}", refreshToken);
            throw new InvalidCredentialsException("토큰이 올바르지 않습니다.");
        }
    }

}
