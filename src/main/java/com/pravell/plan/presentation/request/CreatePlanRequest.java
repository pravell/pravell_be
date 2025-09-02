package com.pravell.plan.presentation.request;

import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreatePlanRequest {

    @NotBlank(message = "플랜 이름은 생략이 불가능합니다.")
    @Size(min = 2, max = 20, message = "플랜 이름은 2 ~ 20자 사이여야 합니다.")
    private String name;

    @NotNull(message = "공개 여부를 지정해야 합니다.")
    private Boolean isPublic;

    public CreatePlanApplicationRequest toApplicationRequest(){
        return CreatePlanApplicationRequest.builder()
                .name(this.name)
                .isPublic(this.isPublic)
                .build();
    }

}
