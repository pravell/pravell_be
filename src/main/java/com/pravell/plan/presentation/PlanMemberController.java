package com.pravell.plan.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.plan.application.PlanMemberFacade;
import com.pravell.plan.application.dto.response.InviteCodeResponse;
import com.pravell.plan.application.dto.response.PlanJoinUserResponse;
import com.pravell.plan.presentation.request.WithdrawFromPlansRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanMemberController {

    private final CommonJwtUtil commonJwtUtil;
    private final PlanMemberFacade planMemberFacade;

    @PostMapping("/{planId}/invite-code")
    public ResponseEntity<InviteCodeResponse> createInviteCode(@PathVariable UUID planId,
                                                               @RequestHeader("authorization") String authorizationHeader){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(planMemberFacade.createInviteCode(planId, id));
    }

    @PostMapping("/join")
    public ResponseEntity<PlanJoinUserResponse> joinUser(@RequestParam String code,
                                                         @RequestHeader("authorization") String authorizationHeader){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        return ResponseEntity.ok(planMemberFacade.join(id, code));
    }

    @DeleteMapping()
    public ResponseEntity<Void> withdrawPlans(@RequestHeader("authorization") String authorizationHeader,
                                            @RequestBody WithdrawFromPlansRequest withdrawFromPlansRequest){
        UUID id = commonJwtUtil.getUserIdFromToken(authorizationHeader);
        planMemberFacade.withdrawPlans(id, withdrawFromPlansRequest.toApplicationRequest());
        return ResponseEntity.noContent().build();
    }
}
