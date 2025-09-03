package com.pravell.plan.application.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WithdrawFromPlansApplicationRequest {
    private List<UUID> planIds;
}
