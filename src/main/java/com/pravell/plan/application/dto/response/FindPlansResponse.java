package com.pravell.plan.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
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

    private List<String> members;
    private LocalDate startDate;
    private LocalDate endDate;

}
