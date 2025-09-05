package com.pravell.marker.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateMarkerApplicationRequest {

    private String color;
    private String description;

}
