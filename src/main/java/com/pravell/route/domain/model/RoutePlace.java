package com.pravell.route.domain.model;

import com.pravell.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "route_places")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class RoutePlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID routeId;

    @Column(nullable = false)
    private Long pinPlaceId;

    private Long sequence;
    private String description;
    private String nickname;
    private LocalDate date;

    public static RoutePlace create(UUID routeId, Long pinPlaceId, Long sequence, String description, String nickname,
                                    LocalDate date) {
        validateDescription(description);
        validateNickname(nickname);

        return RoutePlace.builder()
                .routeId(routeId)
                .pinPlaceId(pinPlaceId)
                .sequence(sequence)
                .description(description)
                .nickname(nickname)
                .date(date)
                .build();
    }

    public void updatePinPlaceId(Long pinPlaceId) {
        this.pinPlaceId = pinPlaceId;
    }

    public void updateDescription(String description) {
        validateDescription(description);
        this.description = description;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    public void updateSequence(Long sequence) {
        this.sequence = sequence;
    }

    public void updateDate(LocalDate date) {
        this.date = date;
    }

    private static void validateDescription(String description) {
        if (description != null && (description.length() < 2 || description.length() > 50)) {
            throw new IllegalArgumentException("장소 메모는 2 ~ 50자여야 합니다.");
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname != null && (nickname.length() < 2 || nickname.length() > 20)) {
            throw new IllegalArgumentException("장소 별명은 2 ~ 20자여야 합니다.");
        }
    }
}
