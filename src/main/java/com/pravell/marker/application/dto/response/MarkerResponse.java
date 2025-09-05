package com.pravell.marker.application.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarkerResponse {

    private Long markerId;
    private UUID planId;
    private String color;
    private String description;

}
