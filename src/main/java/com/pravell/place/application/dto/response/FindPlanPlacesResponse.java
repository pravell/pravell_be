package com.pravell.place.application.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindPlanPlacesResponse {

    private Long id;
    private String nickname;
    private String title;
    private String mapx;
    private String mapy;
    private BigDecimal lat;
    private BigDecimal lng;
    private String pinColor;

}
