package com.pravell.route.application.dto.request;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DeleteRoutePlacesApplicationRequest {

    private List<Long> deleteRoutePlaceId;

}
