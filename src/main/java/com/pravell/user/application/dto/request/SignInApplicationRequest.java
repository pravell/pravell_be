package com.pravell.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignInApplicationRequest {
    String id;
    String password;
}
