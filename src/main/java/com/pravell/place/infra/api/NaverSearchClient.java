package com.pravell.place.infra.api;

import com.pravell.place.application.NaverSearchApi;
import com.pravell.place.application.dto.response.api.NaverPlaceResponse;
import com.pravell.place.application.dto.response.api.NaverSearchResponse;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class NaverSearchClient implements NaverSearchApi {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${naver.search.api.client-id}")
    private String clientId;

    @Value("${naver.search.api.client-secret}")
    private String clientSecret;

    @Value("${naver.search.api.url}")
    private String apiUrl;

    @Override
    public List<NaverPlaceResponse> search(String keyword) {
        String url = apiUrl + keyword;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<NaverSearchResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                NaverSearchResponse.class);

        return Objects.requireNonNull(response.getBody()).getItems();
    }

}
