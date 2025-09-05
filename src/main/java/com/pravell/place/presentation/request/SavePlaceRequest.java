package com.pravell.place.presentation.request;

import com.pravell.place.application.dto.request.SavePlaceApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SavePlaceRequest {

    private String placeId;

    @Size(min = 2, max = 30, message = "nickname은 2 ~ 30자여야 합니다.")
    private String nickname;

    @NotBlank(message = "title은 생략이 불가능합니다.")
    private String title;

    @NotBlank(message = "address는 생략이 불가능합니다.")
    private String address;

    @NotBlank(message = "roadAddress는 생략이 불가능합니다.")
    private String roadAddress;

    private List<String> hours;

    @NotBlank(message = "mapx는 생략이 불가능합니다.")
    private String mapx;

    @NotBlank(message = "mapy는 생략이 불가능합니다.")
    private String mapy;

    @NotNull(message = "latitude는 생략이 불가능합니다.")
    private BigDecimal lat;

    @NotNull(message = "longitude는 생략이 불가능합니다.")
    private BigDecimal lng;

    @NotBlank(message = "pinColor는 생략이 불가능합니다.")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "올바르지 못한 pinColor입니다.")
    private String pinColor;

    @NotNull(message = "planId는 생략이 불가능합니다.")
    private UUID planId;

    @Size(min = 2, max = 255, message = "description은  2 ~ 255자여야 합니다.")
    private String description;

    public SavePlaceApplicationRequest toApplicationRequest(){
        return SavePlaceApplicationRequest.builder()
                .placeId(this.placeId)
                .nickname(this.nickname)
                .title(this.title)
                .address(this.address)
                .roadAddress(this.roadAddress)
                .hours(this.hours)
                .mapx(this.mapx)
                .mapy(this.mapy)
                .lat(this.lat)
                .lng(this.lng)
                .pinColor(this.pinColor)
                .planId(this.planId)
                .description(this.description)
                .build();
    }

}
