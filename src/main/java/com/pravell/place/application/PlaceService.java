package com.pravell.place.application;

import com.pravell.place.application.dto.PlaceDTO;
import com.pravell.place.domain.exception.PlaceNotFoundException;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.repository.PinPlaceRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PinPlaceRepository pinPlaceRepository;

    @Transactional(readOnly = true)
    public PinPlace findPlace(Long placeId) {
        return pinPlaceRepository.findById(placeId)
                .orElseThrow(() -> new PlaceNotFoundException("장소를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public PlaceDTO findPlaceByPlaceIdAndPlanId(Long pinPlaceId, UUID planId) {
        Optional<PinPlace> pinPlace = pinPlaceRepository.findById(pinPlaceId);

        if (pinPlace.isEmpty() || !pinPlace.get().getPlanId().equals(planId)) {
            throw new PlaceNotFoundException("저장된 장소를 찾을 수 없습니다.");
        }

        return PlaceDTO.builder()
                .title(pinPlace.get().getTitle())
                .address(pinPlace.get().getAddress())
                .roadAddress(pinPlace.get().getRoadAddress())
                .mapx(pinPlace.get().getMapx())
                .mapy(pinPlace.get().getMapy())
                .lat(pinPlace.get().getLatitude())
                .lng(pinPlace.get().getLongitude())
                .color(pinPlace.get().getPinColor())
                .build();
    }

}
