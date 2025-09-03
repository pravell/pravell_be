package com.pravell.plan.domain.event;

import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUsers;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PlanCreatedEvent extends PlanEvent {

    private PlanUsers planUsers;

    public PlanCreatedEvent(Plan plan, PlanUsers planUsers, LocalDateTime createdAt) {
        super(plan, createdAt);
        this.planUsers = planUsers;
    }

}
