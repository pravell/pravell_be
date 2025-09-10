package com.pravell.route.presentation.request;

import com.pravell.route.application.dto.request.UpdatePlaceApplicationRequest;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdatePlaceRequest {

    private Long pinPlaceId;

    @Size(min = 2, max = 50, message = "장소 메모는 2 ~ 50자여야 합니다.")
    private String description;

    @Size(min = 2, max = 20, message = "장소 별명은 2 ~ 20자여야 합니다.")
    private String nickname;

    private Long sequence;

    private LocalDate date;

    public UpdatePlaceApplicationRequest toApplicationRequest(){
        return UpdatePlaceApplicationRequest.builder()
                .pinPlaceId(this.pinPlaceId)
                .description(this.description)
                .nickname(this.nickname)
                .sequence(this.sequence)
                .date(this.date)
                .build();
    }

}
