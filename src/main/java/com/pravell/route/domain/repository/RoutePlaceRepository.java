package com.pravell.route.domain.repository;

import com.pravell.route.domain.model.RoutePlace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutePlaceRepository extends JpaRepository<RoutePlace, Long> {

    @Query("SELECT MAX(r.sequence) FROM RoutePlace r WHERE r.routeId = :routeId")
    Optional<Long> findMaxSequenceByRouteId(UUID routeId);

    List<RoutePlace> findAllByRouteId(UUID routeId);
}
