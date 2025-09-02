package com.pravell.user.presentation.request;

import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignUpRequest {

    @NotBlank(message = "아이디는 생략이 불가능합니다.")
    @Size(min = 2, max = 30, message = "아이디는 2 ~ 30자여야 합니다.")
    private String id;

    @NotBlank(message = "비밀번호는 생략이 불가능합니다.")
    @Size(min = 8, max=64, message = "비밀번호는 8 ~ 64자여야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 생략이 불가능합니다.")
    @Size(min = 2, max = 30, message = "닉네임은 2 ~ 30자여야 합니다.")
    private String nickname;

    public SignUpApplicationRequest toSignUpApplicationRequest(){
        return SignUpApplicationRequest.builder()
                .id(this.id)
                .password(this.password)
                .nickname(this.nickname)
                .build();
    }

}
