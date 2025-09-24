package com.pravell.plan.domain.model;

import com.pravell.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "plans")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Plan extends AggregateRoot {

    @Id
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false)
    private Boolean isDeleted;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Override
    public String toString() {
        return "Plan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isPublic=" + isPublic +
                ", isDeleted=" + isDeleted +
                '}';
    }

    public static Plan create(String name, Boolean isPublic, LocalDate startDate, LocalDate endDate) {
        validateName(name);
        validateDate(startDate, endDate);

        return Plan.builder()
                .id(UUID.randomUUID())
                .name(name)
                .isPublic(isPublic)
                .isDeleted(false)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    public void delete() {
        isDeleted = true;
    }

    public void updatePublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateStartDate(LocalDate startDate) {
        validateDate(startDate, this.endDate);
        this.startDate = startDate;
    }

    public void updateEndDate(LocalDate endDate) {
        validateDate(this.startDate, endDate);
        this.endDate = endDate;
    }

    public void updateDate(LocalDate startDate, LocalDate endDate) {
        validateDate(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private static void validateName(String name) {
        if (name == null || name.length() < 2 || name.length() > 20) {
            throw new IllegalArgumentException("플랜 이름은 2 ~ 20자 사이여야 합니다.");
        }
    }

    private static void validateDate(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료 날짜가 시작 날짜보다 앞설 수 없습니다.");
        }
    }
}
