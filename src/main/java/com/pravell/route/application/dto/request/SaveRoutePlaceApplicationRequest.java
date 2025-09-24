package com.pravell.route.application.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class SaveRoutePlaceApplicationRequest {

    private Long pinPlaceId;
    private String description;
    private String nickname;
    private LocalDate date;

}
