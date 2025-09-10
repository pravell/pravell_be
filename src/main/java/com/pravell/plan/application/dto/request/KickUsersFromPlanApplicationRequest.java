package com.pravell.plan.application.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KickUsersFromPlanApplicationRequest {
    private List<UUID> deleteUsers;
}
