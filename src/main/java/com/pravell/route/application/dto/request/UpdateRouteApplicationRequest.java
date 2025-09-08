package com.pravell.route.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRouteApplicationRequest {

    private String name;
    private String description;

}
