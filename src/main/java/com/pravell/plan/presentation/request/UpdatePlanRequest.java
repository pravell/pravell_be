package com.pravell.plan.presentation.request;

import com.pravell.plan.application.dto.request.UpdatePlanApplicationRequest;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UpdatePlanRequest {

    @Size(min = 2, max = 20, message = "플랜 이름은 2 ~ 20자 사이여야 합니다.")
    private String name;

    private Boolean isPublic;

    public UpdatePlanApplicationRequest toApplicationRequest(){
        return UpdatePlanApplicationRequest.builder()
                .name(this.name)
                .isPublic(this.isPublic)
                .build();
    }

}
