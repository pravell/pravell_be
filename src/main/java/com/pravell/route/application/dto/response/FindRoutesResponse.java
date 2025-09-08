package com.pravell.route.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindRoutesResponse {

    private UUID routeId;
    private String name;
    private String description;
    private LocalDateTime createdAt;

}
