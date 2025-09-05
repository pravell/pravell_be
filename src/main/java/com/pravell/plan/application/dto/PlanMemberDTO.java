package com.pravell.plan.application.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanMemberDTO {
    private UUID memberId;
    private String planMemberStatus;
}
