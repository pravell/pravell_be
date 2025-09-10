package com.pravell.user.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.user.application.UserService;
import com.pravell.user.application.dto.response.UserProfileResponse;
import com.pravell.user.presentation.request.UpdateUserRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> withDrawUser(@RequestHeader("Authorization") String authorizationHeader){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        userService.withDrawUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateUser(@RequestHeader("Authorization") String authorizationHeader,
                                                          @Valid @RequestBody UpdateUserRequest updateUserRequest){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(userService.updateUser(id, updateUserRequest.toUpdateUserApplicationRequest()));
    }

}
