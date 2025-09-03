package com.pravell.plan.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UpdatePlanApplicationRequest {

    private String name;
    private Boolean isPublic;

}
