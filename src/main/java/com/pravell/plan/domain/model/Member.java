package com.pravell.plan.domain.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Member {
    private UUID memberId;
    private String nickname;
}
