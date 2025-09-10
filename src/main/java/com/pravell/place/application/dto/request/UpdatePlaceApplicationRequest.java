package com.pravell.place.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdatePlaceApplicationRequest {

    private String nickname;
    private String pinColor;
    private String description;

    @Override
    public String toString() {
        return "UpdatePlaceApplicationRequest{" +
                "nickname='" + nickname + '\'' +
                ", pinColor='" + pinColor + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
