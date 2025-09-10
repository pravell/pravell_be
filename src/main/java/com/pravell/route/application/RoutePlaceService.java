package com.pravell.route.application;

import com.pravell.route.domain.exception.RoutePlaceNotFoundException;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.domain.repository.RoutePlaceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutePlaceService {

    private final RoutePlaceRepository routePlaceRepository;

    @Transactional(readOnly = true)
    public RoutePlace findRoutePlace(Long routePlaceId) {
        return routePlaceRepository.findById(routePlaceId)
                .orElseThrow(() -> new RoutePlaceNotFoundException("루트에서 해당 장소를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public boolean existsRoutePlaceInRoute(UUID routeId, List<Long> deleteRoutePlaceId) {
        long count = routePlaceRepository.countByRouteIdAndIdIn(routeId, deleteRoutePlaceId);
        return count == deleteRoutePlaceId.size();
    }

}
