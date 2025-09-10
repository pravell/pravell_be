package com.pravell.place.application.dto.response.api;

import java.util.List;
import lombok.Data;

@Data
public class GooglePlaceResponse {
    public List<GoogleCandidate> candidates;

    @Data
    public static class GoogleCandidate{
        public String place_id;
    }
}
