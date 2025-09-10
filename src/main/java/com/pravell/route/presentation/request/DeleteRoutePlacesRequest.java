package com.pravell.route.presentation.request;

import com.pravell.route.application.dto.request.DeleteRoutePlacesApplicationRequest;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteRoutePlacesRequest {

    private List<Long> deleteRoutePlaceId;

    public DeleteRoutePlacesApplicationRequest toApplicationRequest(){
        return DeleteRoutePlacesApplicationRequest.builder()
                .deleteRoutePlaceId(this.deleteRoutePlaceId)
                .build();
    }
}
