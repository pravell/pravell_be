package com.pravell.route.application.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DeleteRouteApplicationRequest {

    public List<UUID> routeId;

}
