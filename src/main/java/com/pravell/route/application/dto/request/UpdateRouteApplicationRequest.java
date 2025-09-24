package com.pravell.route.application.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UpdateRouteApplicationRequest {

    private String name;
    private String description;

}
