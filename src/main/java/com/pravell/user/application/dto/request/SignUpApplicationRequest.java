package com.pravell.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpApplicationRequest {

    private String id;
    private String password;
    private String nickname;

}
