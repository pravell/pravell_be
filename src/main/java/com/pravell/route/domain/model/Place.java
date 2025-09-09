package com.pravell.route.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Place {

    private Long pinPlaceId;
    private String title;
    private String address;
    private String roadAddress;
    private String mapx;
    private String mapy;
    private BigDecimal lat;
    private BigDecimal lng;
    private String color;

}
