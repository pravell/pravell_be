package com.pravell.expense.presentation;

import com.pravell.common.util.CommonJwtUtil;
import com.pravell.expense.application.ExpenseFacade;
import com.pravell.expense.application.dto.response.ExpenseResponse;
import com.pravell.expense.presentation.request.CreateExpenseRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/plans/{planId}/expenses")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String header,
            @PathVariable UUID planId,
            @RequestParam(required = false, name = "from") String fromStr,
            @RequestParam(required = false, name = "to") String toStr,
            @RequestParam(required = false, name = "paidUserId") String userIdStr
    ) {
        UUID userId = commonJwtUtil.getUserIdFromToken(header);

        LocalDateTime from = parseDateTimeNullable(fromStr);
        LocalDateTime to = parseDateTimeNullable(toStr);
        UUID paidByUserId = parseUuidNullable(userIdStr);

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("종료 날짜가 시작 날짜보다 앞설 수 없습니다.");
        }

        return ResponseEntity.ok(expenseFacade.getExpenses(userId, planId, from, to, paidByUserId));
    }

    private LocalDateTime parseDateTimeNullable(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(s);
    }

    private UUID parseUuidNullable(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return UUID.fromString(s);
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@RequestHeader("Authorization") String header,
                                              @PathVariable UUID expenseId){
        UUID userId = commonJwtUtil.getUserIdFromToken(header);
        expenseFacade.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(@RequestHeader("Authorization") String header,
                                                      @PathVariable UUID expenseId){
        UUID userId = commonJwtUtil.getUserIdFromToken(header);
        return ResponseEntity.ok(expenseFacade.getExpense(userId, expenseId));
    }

}
