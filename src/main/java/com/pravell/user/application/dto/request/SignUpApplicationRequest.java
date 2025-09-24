package com.pravell.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class SignUpApplicationRequest {

    private String id;
    private String password;
    private String nickname;

}
