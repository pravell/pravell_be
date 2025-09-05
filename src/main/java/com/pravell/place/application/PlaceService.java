package com.pravell.place.application;

import com.pravell.place.domain.exception.PlaceNotFoundException;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.repository.PinPlaceRepository;
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
}
