package com.pravell.place.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.pravell.place.application.dto.response.SearchPlaceResponse;
import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse;
import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse.Geometry;
import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse.Location;
import com.pravell.place.application.dto.response.api.GooglePlaceDetailsResponse.OpeningHours;
import com.pravell.place.application.dto.response.api.NaverPlaceResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchPlaceServiceTest {

    @Mock
    private NaverSearchApi naverSearchApi;

    @Mock
    private GoogleSearchApi googleSearchApi;

    @InjectMocks
    private SearchPlaceService searchPlaceService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String MAP_URL = "https://map.com/";

    @BeforeEach
    void setUp() {
        injectMapUrl(searchPlaceService, MAP_URL);
    }

    private void injectMapUrl(SearchPlaceService service, String mapUrl) {
        try {
            var field = SearchPlaceService.class.getDeclaredField("mapUrl");
            field.setAccessible(true);
            field.set(service, mapUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("search()는")
    class Describe_search {

        @DisplayName("정상적으로 검색 결과를 반환한다.")
        @Test
        void shouldReturnSearchResults() {
            //given
            NaverPlaceResponse naver = createNaverResponse("카페", "경주", "경주");
            GooglePlaceDetailsResponse google = createGoogleResponse(new BigDecimal("37.1234"),
                    new BigDecimal("127.1234"), List.of("월~금: 09:00 - 18:00"));

            given(naverSearchApi.search("카페")).willReturn(List.of(naver));
            given(googleSearchApi.getDetails("카페", "경주")).willReturn(google);

            //when
            List<SearchPlaceResponse> result = searchPlaceService.search("카페", USER_ID);

            //then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("카페");
            assertThat(result.get(0).getLat()).isEqualTo(new BigDecimal("37.1234"));
        }

        @DisplayName("Google 응답이 null이면 Naver의 좌표를 쓴다.")
        @Test
        void shouldFallbackToNaverCoordinates() {
            //given
            NaverPlaceResponse naver = createNaverResponse("카페", "경주", "경주");
            naver.setMapx("1271234000");
            naver.setMapy("371234000");

            given(naverSearchApi.search("카페")).willReturn(List.of(naver));
            given(googleSearchApi.getDetails("카페", "경주")).willReturn(null);

            //when
            List<SearchPlaceResponse> result = searchPlaceService.search("카페", USER_ID);

            //then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLat()).isEqualTo(new BigDecimal("37.123400"));
            assertThat(result.get(0).getLng()).isEqualTo(new BigDecimal("127.123400"));
            assertThat(result.get(0).getHoliday()).contains("정보 없음");
        }

        @DisplayName("Naver 결과가 없으면 빈 리스트를 반환한다.")
        @Test
        void shouldReturnEmptyList() {
            //given
            given(naverSearchApi.search("없는 키워드")).willReturn(List.of());

            //when
            List<SearchPlaceResponse> result = searchPlaceService.search("없는 키워드", USER_ID);

            //then
            assertThat(result).isEmpty();
        }

        @DisplayName("Naver 필드가 null이어도 예외 없이 동작한다.")
        @Test
        void shouldHandleNullFieldsInNaver() {
            //given
            NaverPlaceResponse naver = NaverPlaceResponse.builder().build();

            given(naverSearchApi.search("무효")).willReturn(List.of(naver));
            given(googleSearchApi.getDetails(null, null)).willReturn(null);

            //when
            List<SearchPlaceResponse> result = searchPlaceService.search("무효", USER_ID);

            //then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isBlank();
        }
    }

    private NaverPlaceResponse createNaverResponse(String title, String address, String roanAddress) {
        return NaverPlaceResponse.builder()
                .title(title)
                .address(address)
                .roadAddress(roanAddress)
                .build();
    }

    private GooglePlaceDetailsResponse createGoogleResponse(BigDecimal lat, BigDecimal lng, List<String> openingHours) {
        Location location = new Location(lat, lng);
        Geometry geometry = new Geometry(location);
        OpeningHours oh = new OpeningHours(openingHours);

        return GooglePlaceDetailsResponse.builder()
                .geometry(geometry)
                .opening_hours(oh)
                .build();
    }

}
