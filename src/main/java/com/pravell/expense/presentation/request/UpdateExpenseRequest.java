package com.pravell.expense.presentation.request;

import com.pravell.expense.application.dto.request.UpdateExpenseApplicationRequest;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateExpenseRequest {

    @Size(max = 50, message = "타이틀은 50자까지 가능합니다.")
    private String title;

    @Size(max = 255, message = "설명은 255자까지 가능합니다.")
    private String description;

    private Long amount;
    private UUID paidByUserId;
    private LocalDateTime spentAt;


    public UpdateExpenseApplicationRequest toApplicationRequest(){
        return UpdateExpenseApplicationRequest.builder()
                .title(this.title)
                .amount(this.amount)
                .paidByUserId(this.paidByUserId)
                .spentAt(this.spentAt)
                .description(this.description)
                .build();
    }

}
