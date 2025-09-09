package com.pravell.place.application.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceDTO {

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
