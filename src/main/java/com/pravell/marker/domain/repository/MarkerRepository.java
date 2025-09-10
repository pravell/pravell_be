package com.pravell.marker.domain.repository;

import com.pravell.marker.domain.model.Marker;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarkerRepository extends JpaRepository<Marker, Long> {
    List<Marker> findAllByPlanId(UUID planId);
}
