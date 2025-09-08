package com.pravell.route.presentation.request;

import com.pravell.route.application.dto.request.CreateRouteApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateRouteRequest {

    @NotNull(message = "플랜을 지정해야 합니다.")
    private UUID planId;

    @NotBlank(message = "이름은 생략이 불가능합니다.")
    @Size(min = 2, max = 30, message = "이름은 2 ~ 30자 사이여야 합니다.")
    private String name;

    @Size(min = 2, max = 50, message = "루트 설명은 2 ~ 50자 사이여야 합니다.")
    private String description;

    public CreateRouteApplicationRequest toApplicationRequest(){
        return CreateRouteApplicationRequest.builder()
                .planId(this.planId)
                .name(this.name)
                .description(this.description)
                .build();
    }

}
