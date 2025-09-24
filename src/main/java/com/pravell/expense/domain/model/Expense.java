package com.pravell.expense.domain.model;

import com.pravell.common.domain.AggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Table(name = "expenses")
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Expense extends AggregateRoot {

    @Id
    private UUID id;

    private UUID planId;
    private UUID paidByUserId;
    private Long amount;
    private String title;
    private String description;
    private LocalDateTime spentAt;
    private boolean isDeleted;

}
