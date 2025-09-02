package com.pravell.user.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.user.application.UserService;
import com.pravell.user.application.dto.response.UserProfileResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CommonJwtUtil commonJwtUtil;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader("Authorization") String authorizationHeader){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(userService.getProfile(id));
    }

}
