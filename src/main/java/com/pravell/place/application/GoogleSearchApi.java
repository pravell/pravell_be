package com.pravell.place.application;

import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse;

public interface GoogleSearchApi {

    GooglePlaceDetailsResponse getDetails(String title, String roadAddress);

}
