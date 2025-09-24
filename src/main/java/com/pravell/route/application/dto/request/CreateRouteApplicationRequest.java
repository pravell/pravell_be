package com.pravell.route.application.dto.request;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CreateRouteApplicationRequest {

    private UUID planId;
    private String name;
    private String description;

}
