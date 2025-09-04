package com.pravell.place.domain.repository;

import com.pravell.place.domain.model.PinPlace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PinPlaceRepository extends JpaRepository<PinPlace, Long> {

    boolean existsByPlanIdAndAddress(UUID planId, String address);

}
