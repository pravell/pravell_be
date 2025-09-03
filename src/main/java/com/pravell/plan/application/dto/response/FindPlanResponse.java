package com.pravell.plan.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pravell.plan.domain.model.Member;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindPlanResponse {

    private UUID planId;
    private String name;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private LocalDateTime createdAt;
    private UUID ownerId;
    private String ownerNickname;
    private List<Member> member;

}

