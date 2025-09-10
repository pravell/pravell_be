package com.pravell.route.application;

import com.pravell.route.domain.exception.RouteNotFoundException;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.repository.RouteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public Route findById(UUID id) {
        return routeRepository.findById(id)
                .filter(r->!r.isDeleted())
                .orElseThrow(() -> new RouteNotFoundException("루트를 찾을 수 없습니다."));
    }

}
