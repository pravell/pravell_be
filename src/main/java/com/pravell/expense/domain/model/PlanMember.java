package com.pravell.expense.domain.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class PlanMember {
    private UUID memberId;
    private String nickname;
}
