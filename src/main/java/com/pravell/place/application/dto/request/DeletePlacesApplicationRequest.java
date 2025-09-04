package com.pravell.place.application.dto.request;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeletePlacesApplicationRequest {
    List<Long> placeId;
}
