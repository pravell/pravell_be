package com.pravell.place.application;

import com.pravell.place.application.dto.response.api.NaverPlaceResponse;
import java.util.List;

public interface NaverSearchApi {

    List<NaverPlaceResponse> search(String keyword);

}
