package com.pravell.route.application.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateRouteResponse {

    private UUID routeId;
    private UUID planId;
    private String name;
    private String description;

}
