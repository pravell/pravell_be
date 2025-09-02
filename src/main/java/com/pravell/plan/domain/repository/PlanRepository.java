package com.pravell.plan.domain.repository;

import com.pravell.plan.domain.model.Plan;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
}
