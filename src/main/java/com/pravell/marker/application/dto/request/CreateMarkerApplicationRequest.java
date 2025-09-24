package com.pravell.marker.application.dto.request;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CreateMarkerApplicationRequest {

    private UUID planId;
    private String color;
    private String description;

}
