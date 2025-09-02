package com.pravell.user.application.dto.response;

import com.pravell.user.domain.model.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserProfileResponse {

    private final String userId;
    private final String nickname;
    private final UserStatus status;

}
