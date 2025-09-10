package com.pravell.route.application.dto.request;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdatePlaceApplicationRequest {

    private Long pinPlaceId;
    private String description;
    private String nickname;
    private Long sequence;
    private LocalDate date;

}
