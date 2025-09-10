package com.pravell.place.presentation.request;

import com.pravell.place.application.dto.request.UpdatePlaceApplicationRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdatePlaceRequest {

    @Size(min = 2, max = 30, message = "nickname은 2 ~ 30자여야 합니다.")
    private String nickname;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "올바르지 않은 pin color입니다.")
    private String pinColor;

    @Size(min = 2, max = 255, message = "description은 2 ~ 255자여야 합니다.")
    private String description;

    public UpdatePlaceApplicationRequest toApplicationRequest(){
        return UpdatePlaceApplicationRequest.builder()
                .nickname(this.nickname)
                .pinColor(this.pinColor)
                .description(this.description)
                .build();
    }

}
