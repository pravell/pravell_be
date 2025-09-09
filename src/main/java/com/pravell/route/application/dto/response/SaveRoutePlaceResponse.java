package com.pravell.route.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SaveRoutePlaceResponse {

    private Long routePlaceId;
    private Long pinPlaceId;
    private String title;
    private String nickname;
    private String description;
    private Long sequence;
    private LocalDate date;
    private String address;
    private String roadAddress;
    private String mapx;
    private String mapy;
    private BigDecimal lat;
    private BigDecimal lng;
    private String color;

}
