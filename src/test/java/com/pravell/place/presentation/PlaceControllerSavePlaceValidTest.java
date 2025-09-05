package com.pravell.place.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pravell.ControllerTestSupport;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.place.presentation.request.SavePlaceRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlaceControllerSavePlaceValidTest extends ControllerTestSupport {

    @Autowired
    private PinPlaceRepository pinPlaceRepository;

    @AfterEach
    void tearDown() {
        pinPlaceRepository.deleteAllInBatch();
    }

    @DisplayName("닉네임이 2자 미만이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNicknameIsTooShort() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("n")
                .title("title")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: nickname은 2 ~ 30자여야 합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("닉네임이 30자 초과면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenNicknameameIsTooLong() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("n".repeat(31))
                .title("title")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("nickname: nickname은 2 ~ 30자여야 합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("장소 이름이 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenTitleIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: title은 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("장소 이름이 공백이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenTitleIsBlank() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("   ")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("title: title은 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("주소가 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenAddressIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("address: address는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("주소가 공백이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenAddressIsBlank() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("    ")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("address: address는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("도로명 주소가 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenRoadAddressIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("roadAddress: roadAddress는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("도로명 주소가 공백이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenRoadAddressIsBlank() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("    ")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("roadAddress: roadAddress는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("mapX가 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenMapxIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("mapx: mapx는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("mapX가 공백이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenMapxIsBlank() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("    ")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("mapx: mapx는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("mapy가 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenMapyIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("mapy: mapy는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("mapy가 공백이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenMapyIsBlank() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("   ")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("mapy: mapy는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("마커 색상이 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenPinColorIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("pinColor: pinColor는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("마커 색상이 HEX code 형식이 아니라면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenPinColorIsNotHexFormat() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("2314")
                .planId(UUID.randomUUID())
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("pinColor: 올바르지 못한 pin color입니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("플랜 id가 Null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenPlanIdIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .description("description")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("planId: planId는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("description이 2자 미만이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsTooShort() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("d")
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은  2 ~ 255자여야 합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("description이 255자 초과면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenDescriptionIsTooLong() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("d".repeat(256))
                .lat(new BigDecimal("35.8234094"))
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("description: description은  2 ~ 255자여야 합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("lat이 null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenLatIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("desc")
                .lng(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("lat: latitude는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

    @DisplayName("lng이 null이면 장소를 저장하지 못하고, 400을 반환한다.")
    @Test
    void shouldReturn400_whenLngIsNull() throws Exception {
        //given
        SavePlaceRequest request = SavePlaceRequest.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("경주카페")
                .address("경상북도 경주시 탑동 xxx")
                .roadAddress("경상북도 경주시 탑동 xxx")
                .hours(List.of("hours"))
                .mapx("1292108392")
                .mapy("358234152")
                .pinColor("#F54927")
                .planId(UUID.randomUUID())
                .description("desc")
                .lat(new BigDecimal("129.2108357"))
                .build();

        String token = buildToken(UUID.randomUUID(), "access", issuer, Instant.now().plusSeconds(10000));

        //when, then
        mockMvc.perform(
                        post("/api/v1/places")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("lng: longitude는 생략이 불가능합니다."));

        assertThat(pinPlaceRepository.count()).isZero();
    }

}
