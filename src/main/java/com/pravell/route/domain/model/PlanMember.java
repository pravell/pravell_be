package com.pravell.route.domain.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanMember {
    private UUID memberId;
    private PlanMemberStatus planMemberStatus;
}
