package com.pravell.expense.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpenseResponse {

    private UUID expenseId;
    private String title;
    private Long amount;
    private UUID paidByUserId;
    private LocalDateTime spentAt;
    private String description;
    private String paidByUserNickname;

}
