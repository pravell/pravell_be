package com.pravell.marker.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindMarkersResponse {

    private String color;
    private String description;

}
