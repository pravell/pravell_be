package com.pravell.plan.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FindPlansResponse {

    private UUID planId;
    private String planName;
    @JsonProperty("isOwner")
    private boolean isOwner;

}
