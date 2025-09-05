package com.pravell.marker.application;

import com.pravell.marker.domain.exception.MarkerNotFoundException;
import com.pravell.marker.domain.model.Marker;
import com.pravell.marker.domain.repository.MarkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarkerService {

    private final MarkerRepository markerRepository;

    @Transactional(readOnly = true)
    public Marker findMarker(Long markerId) {
        return markerRepository.findById(markerId)
                .orElseThrow(()->new MarkerNotFoundException("마커를 찾을 수 없습니다."));
    }

}
