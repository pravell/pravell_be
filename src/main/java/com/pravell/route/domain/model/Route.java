package com.pravell.route.domain.model;

import com.pravell.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routes")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route extends AggregateRoot {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private String name;

    private String description;
    private boolean isDeleted;

    public static Route create(UUID planId, String name, String description) {
        validateCreate(planId, name, description);

        return Route.builder()
                .id(UUID.randomUUID())
                .planId(planId)
                .name(name)
                .description(description)
                .isDeleted(false)
                .build();
    }

    private static void validateCreate(UUID planId, String name, String description) {
        validatePlanId(planId);
        validateName(name);
        validateDescription(description);
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    public void updateDescription(String description) {
        validateDescription(description);
        this.description = description;
    }

    private static void validatePlanId(UUID planId) {
        if (planId == null) {
            throw new IllegalArgumentException("플랜을 지정해야 합니다.");
        }
    }

    private static void validateName(String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("이름은 생략이 불가능합니다.");
        }
        if (name.length() < 2 || name.length() > 30) {
            throw new IllegalArgumentException("이름은 2 ~ 30자 사이여야 합니다.");
        }
    }

    private static void validateDescription(String description) {
        if (description != null && (description.length() < 2 || description.length() > 50)) {
            throw new IllegalArgumentException("루트 설명은 2 ~ 50자 사이여야 합니다.");
        }
    }
}
