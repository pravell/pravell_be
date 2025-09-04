package com.pravell.place.domain.model;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanMember {
    private UUID memberId;
    private String planMemberStatus;
}
