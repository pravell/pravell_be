package com.pravell.place.application.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceResponse {

    private Long id;
    private String nickname;
    private String title;
    private String address;
    private String roadAddress;
    private List<String> hours;
    private String mapx;
    private String mapy;
    private BigDecimal lat;
    private BigDecimal lng;
    private String pinColor;
    private UUID planId;
    private String description;
    private String mapUrl;

}
