package com.pravell.plan.application;

import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
import com.pravell.plan.application.dto.response.CreatePlanResponse;
import com.pravell.plan.application.dto.response.FindPlansResponse;
import com.pravell.plan.domain.event.PlanCreatedEvent;
import com.pravell.user.application.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanFacade {

    private final UserService userService;
    private final CreatePlanService createPlanService;
    private final FindPlanService findPlanService;

    public CreatePlanResponse createPlan(CreatePlanApplicationRequest request, UUID id){
        userService.findUserById(id);

        PlanCreatedEvent planCreatedEvent = createPlanService.create(request, id);

        return CreatePlanResponse.builder()
                .planId(planCreatedEvent.getPlan().getId())
                .createdAt(planCreatedEvent.getCreatedAt())
                .isPublic(planCreatedEvent.getPlan().getIsPublic())
                .name(planCreatedEvent.getPlan().getName())
                .build();
    }

    public List<FindPlansResponse> findAllPlans(UUID id) {
        userService.findUserById(id);
        return findPlanService.findAll(id);
    }
}
