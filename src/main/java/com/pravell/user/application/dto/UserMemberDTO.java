package com.pravell.user.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserMemberDTO {

    private UUID memberId;
    private String nickname;

}
