package com.pravell.plan.application;

import com.pravell.plan.application.dto.request.CreatePlanApplicationRequest;
import com.pravell.plan.domain.event.PlanCreatedEvent;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.plan.domain.service.PlanCreateService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePlanService {

    private final PlanRepository planRepository;
    private final PlanUsersRepository planUsersRepository;
    private final PlanCreateService planCreateService;

    @Transactional
    public PlanCreatedEvent create(CreatePlanApplicationRequest request, UUID id) {
        PlanCreatedEvent planCreatedEvent = planCreateService.create(request, id);

        log.info("{} 유저가 {} 플랜을 생성.", id, planCreatedEvent.getPlan().getId());

        planRepository.save(planCreatedEvent.getPlan());
        planUsersRepository.save(planCreatedEvent.getPlanUsers());

        return planCreatedEvent;
    }

}
