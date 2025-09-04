package com.pravell.place.application.dto.response.api;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GooglePlaceDetailsResponse {

    private Geometry geometry;
    private OpeningHours opening_hours;

    public BigDecimal getLatitude() {
        return geometry.location.lat;
    }

    public BigDecimal getLongitude() {
        return geometry.location.lng;
    }

    public List<String> getOpeningHours() {
        return opening_hours != null ? opening_hours.weekday_text : List.of("정보 없음");
    }

    @Data
    @AllArgsConstructor
    public static class Geometry {
        private Location location;
    }

    @Data
    @AllArgsConstructor
    public static class Location {
        private BigDecimal lat;
        private BigDecimal lng;
    }

    @Data
    @AllArgsConstructor
    public static class OpeningHours {
        private List<String> weekday_text;
    }
}
