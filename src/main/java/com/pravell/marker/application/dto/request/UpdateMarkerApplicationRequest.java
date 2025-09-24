package com.pravell.marker.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UpdateMarkerApplicationRequest {

    private String color;
    private String description;

}
