package com.pravell.expense.domain.model;

import com.pravell.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Table(
        name = "expenses",
        indexes = {
                @Index(name = "idx_expenses_plan_spent", columnList = "plan_id, spent_at"),
                @Index(name = "idx_expenses_paid_by", columnList = "paid_by_user_id")
        }
)
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Expense extends AggregateRoot {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID planId;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID paidByUserId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime spentAt;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    protected UUID createdBy;

    private String description;
    private boolean isDeleted;


    public static Expense create(UUID planId, UUID paidByUserId, Long amount, String title, String description,
                                 LocalDateTime spentAt, UUID userId) {
        validateCreateExpense(planId, paidByUserId, amount, spentAt, userId, title, description);

        return Expense.builder()
                .id(UUID.randomUUID())
                .planId(planId)
                .paidByUserId(paidByUserId)
                .amount(amount)
                .title(title)
                .spentAt(spentAt)
                .createdBy(userId)
                .description(description)
                .isDeleted(false)
                .build();
    }

    private static void validateCreateExpense(UUID planId, UUID paidByUserId, Long amount, LocalDateTime spentAt,
                                              UUID userId, String title, String description) {
        if (planId == null) {
            throw new IllegalArgumentException("planId는 필수입니다.");
        }
        if (paidByUserId == null) {
            throw new IllegalArgumentException("paidByUserId는 필수입니다.");
        }
        if (spentAt == null) {
            throw new IllegalArgumentException("spentAt은 필수입니다.");
        }
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("amount는 0 이상이어야 합니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("createdBy는 필수입니다.");
        }

        validateTitle(title);
        validateDescription(description);
    }

    private static void validateTitle(String title) {
        String t = title == null ? null : title.trim();
        if (t == null || t.isEmpty()) {
            throw new IllegalArgumentException("타이틀은 생략이 불가능합니다.");
        }
        if (t.length() > 50) {
            throw new IllegalArgumentException("타이틀은 50자까지 가능합니다.");
        }
    }

    private static void validateDescription(String description) {
        String d = description == null ? null : description.trim();
        if (d != null && d.length() > 255) {
            throw new IllegalArgumentException("설명은 255자까지 가능합니다.");
        }
    }

}
