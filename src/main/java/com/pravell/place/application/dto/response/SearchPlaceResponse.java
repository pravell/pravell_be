package com.pravell.place.application.dto.response;

import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse;
import com.pravell.place.application.dto.response.api.NaverPlaceResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchPlaceResponse {

    private String title;
    private String address;
    private String roadAddress;
    private List<String> holiday;
    private BigDecimal lat;
    private BigDecimal lng;
    private String mapx;
    private String mapy;
    private String mapUrl;

    public static SearchPlaceResponse of(NaverPlaceResponse n, GooglePlaceDetailsResponse g, String mapUrl) {
        List<String> holidays = Optional.ofNullable(g)
                .map(GooglePlaceDetailsResponse::getOpeningHours)
                .orElse(List.of("정보 없음"));

        BigDecimal lat = Optional.ofNullable(g)
                .map(GooglePlaceDetailsResponse::getLatitude)
                .orElse(null);

        BigDecimal lng = Optional.ofNullable(g)
                .map(GooglePlaceDetailsResponse::getLongitude)
                .orElse(null);

        if (lat == null || lng == null) {
            lat = n.getLatitude();
            lng = n.getLongitude();
        }

        return new SearchPlaceResponse(
                n.cleanTitle(),
                n.getAddress(),
                n.getRoadAddress(),
                holidays,
                lat,
                lng,
                n.getMapx(),
                n.getMapy(),
                mapUrl
        );
    }

}
