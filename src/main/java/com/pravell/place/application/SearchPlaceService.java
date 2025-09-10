package com.pravell.place.application;

import com.pravell.place.application.dto.response.SearchPlaceResponse;
import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse;
import com.pravell.place.application.dto.response.api.NaverPlaceResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchPlaceService {

    private final NaverSearchApi naverSearchApi;
    private final GoogleSearchApi googleSearchApi;

    @Value("${naver.map.url}")
    private String mapUrl;

    public List<SearchPlaceResponse> search(String keyword, UUID id) {
        log.info("{} 유저가 {} 키워드로 검색.", id, keyword);

        List<NaverPlaceResponse> naverResults = naverSearchApi.search(keyword);

        return naverResults.stream().map(n -> {
            GooglePlaceDetailsResponse response = googleSearchApi.getDetails(n.getTitle(), n.getRoadAddress());
            String url = mapUrl + n.cleanTitle().replaceAll("\\s+", "");

            return SearchPlaceResponse.of(n, response, url, response == null ? null : response.getPlaceId());
        }).toList();
    }

}
