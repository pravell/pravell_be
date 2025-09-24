package com.pravell.marker.domain.model;

import com.pravell.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "markers")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Marker extends AggregateRoot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private UUID planId;

    @Override
    public String toString() {
        return "Marker{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                ", planId=" + planId +
                '}';
    }

    public static Marker createMarker(String description, String color, UUID planId) {
        validateDescription(description);
        validateColor(color);
        validatePlanId(planId);

        return Marker.builder()
                .description(description)
                .color(color)
                .planId(planId)
                .build();
    }

    public void updateColor(String color) {
        validateColor(color);
        this.color = color;
    }

    public void updateDescription(String description) {
        validateDescription(description);
        this.description = description;
    }

    private static void validateDescription(String description) {
        if (description == null || description.length() < 2 || description.length() > 30) {
            throw new IllegalArgumentException("description은 2 ~ 30자여야 합니다.");
        }
    }

    private static void validateColor(String color) {
        if (color == null || !color.matches("^#([A-Fa-f0-9]{6})$")) {
            throw new IllegalArgumentException("올바른 HEX 색상 코드 형식이 아닙니다.");
        }
    }

    private static void validatePlanId(UUID planId) {
        if (planId == null) {
            throw new IllegalArgumentException("planId는 생략이 불가능합니다.");
        }
    }
}
