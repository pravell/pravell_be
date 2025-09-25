package com.pravell.expense.presentation.request;

import com.pravell.expense.application.dto.request.CreateExpenseApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateExpenseRequest {

    @NotBlank(message = "타이틀은 생략이 불가능합니다.")
    @Size(max = 50, message = "타이틀은 50자까지 가능합니다.")
    private String title;

    @NotNull(message = "결제 금액은 생략이 불가능합니다.")
    private Long amount;

    @NotNull(message = "결제한 유저는 생략이 불가능합니다.")
    private UUID paidByUserId;

    @NotNull(message = "결제 시각은 생략이 불가능합니다.")
    private LocalDateTime spentAt;

    @Size(max = 255, message = "설명은 255자까지 가능합니다.")
    private String description;

    public CreateExpenseApplicationRequest toApplicationRequest() {
        return CreateExpenseApplicationRequest.builder()
                .title(this.title)
                .amount(this.amount)
                .paidByUserId(this.paidByUserId)
                .spentAt(this.spentAt)
                .description(this.description)
                .build();
    }

}
