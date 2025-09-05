package com.pravell.place.application.dto.response.api;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NaverPlaceResponse {
    private String title;
    private String address;
    private String roadAddress;
    private String mapx;
    private String mapy;
    private String link;

    public String cleanTitle() {
        return title == null ? "" : title.replaceAll("<[^>]*>", "");
    }

    public BigDecimal getLongitude() {
        if (mapx == null) return BigDecimal.ZERO;
        return new BigDecimal(mapx).divide(BigDecimal.valueOf(1e7));
    }

    public BigDecimal getLatitude() {
        if (mapy == null) return BigDecimal.ZERO;
        return new BigDecimal(mapy).divide(BigDecimal.valueOf(1e7));
    }

}
