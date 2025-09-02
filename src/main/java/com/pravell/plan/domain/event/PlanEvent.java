package com.pravell.plan.domain.event;

import com.pravell.common.domain.event.DomainEvent;
import com.pravell.plan.domain.model.Plan;
import java.time.ZonedDateTime;
import lombok.Getter;

@Getter
public abstract class PlanEvent implements DomainEvent<Plan> {

    private final Plan plan;
    private final ZonedDateTime createdAt;

    public PlanEvent(Plan plan, ZonedDateTime createdAt) {
        this.plan = plan;
        this.createdAt = createdAt;
    }

}
