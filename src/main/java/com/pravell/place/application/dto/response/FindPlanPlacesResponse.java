package com.pravell.place.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
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
    private String address;
    private String roadAddress;
    private List<String> hours;

}
