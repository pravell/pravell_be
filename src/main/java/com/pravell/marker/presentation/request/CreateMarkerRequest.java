package com.pravell.marker.presentation.request;

import com.pravell.marker.application.dto.request.CreateMarkerApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateMarkerRequest {

    @NotNull(message = "planId는 생략이 불가능합니다.")
    private UUID planId;

    @NotBlank(message = "pin color는 생략이 불가능합니다.")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "올바른 HEX 색상 코드 형식이 아닙니다.")
    private String color;

    @NotBlank(message = "description은 생략이 불가능합니다.")
    @Size(min = 2, max = 30, message = "description은 2 ~ 30자여야 합니다.")
    private String description;

    public CreateMarkerApplicationRequest toApplicationRequest(){
        return CreateMarkerApplicationRequest.builder()
                .planId(this.planId)
                .color(this.color)
                .description(this.description)
                .build();
    }

}
