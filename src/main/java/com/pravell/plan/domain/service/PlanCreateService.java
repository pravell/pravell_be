package com.pravell.plan.domain.service;

import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
import com.pravell.plan.domain.event.PlanCreatedEvent;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUsers;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PlanCreateService {

    public PlanCreatedEvent create(CreatePlanApplicationRequest request, UUID id){
        Plan plan = Plan.create(request.getName(), request.getIsPublic(), request.getStartDate(), request.getEndDate());
        PlanUsers planUsers = PlanUsers.createOwnerForPlan(id, plan.getId());

        return new PlanCreatedEvent(plan, planUsers, LocalDateTime.now());
    }

}
