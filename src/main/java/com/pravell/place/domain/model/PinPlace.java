package com.pravell.place.domain.model;

import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse;
import com.pravell.place.application.dto.response.api.NaverPlaceResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pin_places")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PinPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String placeId;
    private String nickname;
    private String title;
    private String address;
    private String roadAddress;
    private String mapx;
    private String mapy;
    private String pinColor;
    private UUID planId;
    private UUID savedUser;
    private LocalDateTime lastRefreshedAt;
    private String description;

    @Lob
    @Column
    private String hours;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Override
    public String toString() {
        return "PinPlace{" +
                "id=" + id +
                ", placeId='" + placeId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", title='" + title + '\'' +
                ", address='" + address + '\'' +
                ", roadAddress='" + roadAddress + '\'' +
                ", mapx='" + mapx + '\'' +
                ", mapy='" + mapy + '\'' +
                ", pinColor='" + pinColor + '\'' +
                ", planId=" + planId +
                ", savedUser=" + savedUser +
                ", lastRefreshedAt=" + lastRefreshedAt +
                ", description='" + description + '\'' +
                ", hours='" + hours + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public void updateFromApis(NaverPlaceResponse naver, GooglePlaceDetailsResponse google, String hours) {
        this.title = naver.getTitle();
        this.address = naver.getAddress();
        this.roadAddress = naver.getRoadAddress();
        this.latitude = google.getLatitude();
        this.longitude = google.getLongitude();
        this.hours = hours;
        this.lastRefreshedAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        if (nickname.length() < 2 || nickname.length() > 30) {
            throw new IllegalArgumentException("nickname은 2 ~ 30자여야 합니다.");
        }
        this.nickname = nickname;
    }

    public void updatePinColor(String pinColor) {
        if (!pinColor.matches("^#([A-Fa-f0-9]{6})$")) {
            throw new IllegalArgumentException("올바른 HEX 색상 코드 형식이 아닙니다.");
        }
        this.pinColor = pinColor;
    }

    public void updateDescription(String description) {
        if (nickname.length() < 2 || nickname.length() > 255) {
            throw new IllegalArgumentException("description은 2 ~ 255자여야 합니다.");
        }
        this.description = description;
    }

}
