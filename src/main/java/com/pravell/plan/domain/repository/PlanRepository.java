package com.pravell.plan.domain.repository;

import com.pravell.plan.domain.model.Plan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findAllByIdIn(List<UUID> ids);
}
