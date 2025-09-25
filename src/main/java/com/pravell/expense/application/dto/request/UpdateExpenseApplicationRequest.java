package com.pravell.expense.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UpdateExpenseApplicationRequest {

    private String title;
    private Long amount;
    private UUID paidByUserId;
    private LocalDateTime spentAt;
    private String description;

}
