package com.pravell.plan.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.plan.application.PlanFacade;
import com.pravell.plan.application.dto.response.CreatePlanResponse;
import com.pravell.plan.presentation.request.CreatePlanRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final CommonJwtUtil commonJwtUtil;
    private final PlanFacade planFacade;

    @PostMapping
    public ResponseEntity<CreatePlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest createPlanRequest,
                                                         @RequestHeader("Authorization") String authorizationHeader) {
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        CreatePlanResponse response = planFacade.createPlan(createPlanRequest.toApplicationRequest(), id);

        return ResponseEntity.created(URI.create("/plans/" + response.getPlanId())).body(response);
    }

}
