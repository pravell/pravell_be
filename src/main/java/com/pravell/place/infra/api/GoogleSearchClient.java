package com.pravell.place.infra.api;

import com.pravell.place.application.GoogleSearchApi;
import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse;
import com.pravell.place.application.dto.response.api.GooglePlaceResponse;
import com.pravell.place.application.dto.response.api.GoogleSearchResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GoogleSearchClient implements GoogleSearchApi {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.api.search.url-prefix}")
    private String searchUrlPrefix;

    @Value("${google.api.search.url-params}")
    private String searchUrlParams;

    @Value("${google.api.detail.url-prefix}")
    private String detailUrlPrefix;

    @Value("${google.api.detail.url-params}")
    private String detailUrlParams;

    @Override
    public GooglePlaceDetailsResponse getDetails(String title, String roadAddress) {
        String query = title + " " + roadAddress;
        String searchUrl = searchUrlPrefix + query + searchUrlParams + apiKey;

        ResponseEntity<GooglePlaceResponse> response = restTemplate.getForEntity(searchUrl, GooglePlaceResponse.class);

        String placeId = Optional.ofNullable(response.getBody())
                .flatMap(r -> r.candidates.stream().findFirst())
                .map(c -> c.place_id)
                .orElse(null);

        if (placeId == null) {
            return null;
        }

        String detailUrl = detailUrlPrefix + placeId + detailUrlParams + apiKey;

        ResponseEntity<GoogleSearchResponse> detailResponse = restTemplate.getForEntity(detailUrl,
                GoogleSearchResponse.class);

        GooglePlaceDetailsResponse googlePlaceDetailsResponse = Optional.ofNullable(detailResponse.getBody())
                .map(GoogleSearchResponse::getResult)
                .orElse(null);

        if (googlePlaceDetailsResponse!=null){
            googlePlaceDetailsResponse.setPlaceId(placeId);
        }

        return googlePlaceDetailsResponse;
    }
}
