package com.pravell.route.presentation.request;

import com.pravell.route.application.dto.request.SaveRoutePlaceApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SaveRoutePlaceRequest {

    @NotNull(message = "저장 할 장소는 생략이 불가능합니다.")
    private Long pinPlaceId;

    @Size(min = 2, max = 50, message = "장소 메모는 2 ~ 50자여야 합니다.")
    private String description;

    @Size(min = 2, max = 20, message = "장소 별명은 2 ~ 20자여야 합니다.")
    private String nickname;

    @NotNull(message = "방문 할 날짜는 생략이 불가능합니다.")
    private LocalDate date;

    public SaveRoutePlaceApplicationRequest toApplicationRequest(){
        return SaveRoutePlaceApplicationRequest.builder()
                .pinPlaceId(this.pinPlaceId)
                .description(this.description)
                .nickname(this.nickname)
                .date(this.date)
                .build();
    }

}
