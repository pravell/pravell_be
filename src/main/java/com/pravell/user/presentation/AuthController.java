package com.pravell.user.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.user.application.AuthFacade;
import com.pravell.user.application.dto.response.TokenResponse;
import com.pravell.user.presentation.request.SignInRequest;
import com.pravell.user.presentation.request.SignUpRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;
    private final CommonJwtUtil commonJwtUtil;

    @PostMapping("/sign-up")
    public ResponseEntity<TokenResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        return ResponseEntity.ok(authFacade.signUp(signUpRequest.toSignUpApplicationRequest()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<TokenResponse> signIn(@Valid @RequestBody SignInRequest signInRequest,
                                                HttpServletResponse httpServletResponse) {
        TokenResponse tokenResponse = authFacade.signIn(signInRequest.toSignInApplicationRequest());

        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(14))
                .build();
        httpServletResponse.addHeader("Set-Cookie", responseCookie.toString());

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(@RequestHeader("Authorization") String authorizationHeader,
                                        @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                        HttpServletResponse httpServletResponse) {
        UUID userId = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authFacade.signOut(userId, refreshToken);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();
        httpServletResponse.addHeader("Set-Cookie", deleteCookie.toString());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpServletResponse) {

        TokenResponse rotated = authFacade.rotateRefreshAndIssueAccess(refreshToken);

        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", rotated.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .maxAge(Duration.ofDays(14))
                .build();
        httpServletResponse.addHeader("Set-Cookie", responseCookie.toString());

        return ResponseEntity.ok(rotated);
    }

}
