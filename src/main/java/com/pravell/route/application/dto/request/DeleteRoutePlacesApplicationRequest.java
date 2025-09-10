package com.pravell.route.application.dto.request;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteRoutePlacesApplicationRequest {

    private List<Long> deleteRoutePlaceId;

}
