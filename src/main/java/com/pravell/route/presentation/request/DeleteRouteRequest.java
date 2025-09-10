package com.pravell.route.presentation.request;

import com.pravell.route.application.dto.request.DeleteRouteApplicationRequest;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteRouteRequest {

    public List<UUID> routeId;

    public DeleteRouteApplicationRequest toApplicationRequest() {
        return DeleteRouteApplicationRequest.builder()
                .routeId(routeId)
                .build();
    }

}
