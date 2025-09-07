package com.pravell.plan.application.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreatePlanApplicationRequest {

    private String name;
    private Boolean isPublic;
    private LocalDate startDate;
    private LocalDate endDate;

}
