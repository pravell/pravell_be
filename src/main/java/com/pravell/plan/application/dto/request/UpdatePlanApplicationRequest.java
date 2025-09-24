package com.pravell.plan.application.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class UpdatePlanApplicationRequest {

    private String name;
    private Boolean isPublic;
    private LocalDate startDate;
    private LocalDate endDate;

}
