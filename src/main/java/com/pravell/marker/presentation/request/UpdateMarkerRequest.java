package com.pravell.marker.presentation.request;

import com.pravell.marker.application.dto.request.UpdateMarkerApplicationRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateMarkerRequest {

    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "올바른 HEX 색상 코드 형식이 아닙니다.")
    private String color;

    @Size(min = 2, max = 30, message = "description은 2 ~ 30자여야 합니다.")
    private String description;

    public UpdateMarkerApplicationRequest toApplicationRequest(){
        return UpdateMarkerApplicationRequest.builder()
                .color(this.color)
                .description(this.description)
                .build();
    }

}
