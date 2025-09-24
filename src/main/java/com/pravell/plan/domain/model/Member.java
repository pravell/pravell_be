package com.pravell.plan.domain.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Member {
    private UUID memberId;
    private String nickname;
}
