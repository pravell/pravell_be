package com.pravell.expense.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.expense.application.ExpenseFacade;
import com.pravell.expense.presentation.request.CreateExpenseRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ExpenseController {

    private final CommonJwtUtil commonJwtUtil;
    private final ExpenseFacade expenseFacade;

    @PostMapping("/plans/{planId}/expenses")
    public ResponseEntity<Void> createExpense(@RequestHeader("Authorization") String header,
                                              @Valid @RequestBody CreateExpenseRequest createExpenseRequest,
                                              @PathVariable UUID planId) {
        UUID userId = commonJwtUtil.getUserIdFromToken(header);
        UUID expenseId = expenseFacade.createExpense(userId, planId, createExpenseRequest.toApplicationRequest());
        return ResponseEntity.created(URI.create("/expenses/" + expenseId)).build();
    }
}
