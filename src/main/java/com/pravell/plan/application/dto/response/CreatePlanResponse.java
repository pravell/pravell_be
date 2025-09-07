package com.pravell.plan.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreatePlanResponse {

    private UUID planId;
    private String name;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDate startDate;
    private LocalDate endDate;

}
