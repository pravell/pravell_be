package com.pravell.plan.presentation.request;

import com.pravell.plan.application.dto.request.KickUsersFromPlanApplicationRequest;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class KickUsersFromPlanRequest {
    private List<UUID> deleteUsers;

    public KickUsersFromPlanApplicationRequest toApplicationRequest(){
        return KickUsersFromPlanApplicationRequest.builder()
                .deleteUsers(this.deleteUsers)
                .build();
    }
}
