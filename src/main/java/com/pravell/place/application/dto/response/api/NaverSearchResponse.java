package com.pravell.place.application.dto.response.api;

import java.util.List;
import lombok.Data;

@Data
public class NaverSearchResponse {
    List<NaverPlaceResponse> items;
}
