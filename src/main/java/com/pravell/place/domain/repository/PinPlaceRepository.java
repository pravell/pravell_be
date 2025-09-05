package com.pravell.place.domain.repository;

import com.pravell.place.domain.model.PinPlace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PinPlaceRepository extends JpaRepository<PinPlace, Long> {

    boolean existsByPlanIdAndAddress(UUID planId, String address);

    List<PinPlace> findAllByPlanId(UUID planId);
}
