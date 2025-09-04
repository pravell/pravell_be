package com.pravell.place.presentation.request;

import com.pravell.place.application.dto.request.DeletePlacesApplicationRequest;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeletePlacesRequest {

    List<Long> placeId;

    public DeletePlacesApplicationRequest toApplicationRequest(){
        return DeletePlacesApplicationRequest.builder()
                .placeId(this.placeId)
                .build();
    }

}
