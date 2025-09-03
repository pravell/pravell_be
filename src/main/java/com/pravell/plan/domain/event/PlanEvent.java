package com.pravell.plan.domain.event;

import com.pravell.common.domain.event.DomainEvent;
import com.pravell.plan.domain.model.Plan;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public abstract class PlanEvent implements DomainEvent<Plan> {

    private final Plan plan;
    private final LocalDateTime createdAt;

    public PlanEvent(Plan plan, LocalDateTime createdAt) {
        this.plan = plan;
        this.createdAt = createdAt;
    }

}
