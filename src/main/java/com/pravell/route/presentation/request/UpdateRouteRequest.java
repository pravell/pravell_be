package com.pravell.route.presentation.request;

import com.pravell.route.application.dto.request.UpdateRouteApplicationRequest;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRouteRequest {

    @Size(min = 2, max = 30, message = "이름은 2 ~ 30자 사이여야 합니다.")
    private String name;

    @Size(min = 2, max = 50, message = "루트 설명은 2 ~ 50자 사이여야 합니다.")
    private String description;

    public UpdateRouteApplicationRequest toApplicationRequest(){
        return UpdateRouteApplicationRequest.builder()
                .name(this.name)
                .description(this.description)
                .build();
    }

}
