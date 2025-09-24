package com.pravell.place.application.dto.request;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DeletePlacesApplicationRequest {
    List<Long> placeId;
}
