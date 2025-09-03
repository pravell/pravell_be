package com.pravell.plan.presentation.request;

import com.pravell.plan.application.dto.request.WithdrawFromPlansApplicationRequest;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class WithdrawFromPlansRequest {
    private List<UUID> planIds;

    public WithdrawFromPlansApplicationRequest toApplicationRequest(){
        return WithdrawFromPlansApplicationRequest.builder()
                .planIds(this.planIds)
                .build();
    }

}
