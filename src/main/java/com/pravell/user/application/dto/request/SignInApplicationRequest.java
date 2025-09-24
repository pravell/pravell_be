package com.pravell.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class SignInApplicationRequest {
    String id;
    String password;
}
