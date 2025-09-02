package com.pravell.plan.domain.repository;

import com.pravell.plan.domain.model.PlanUsers;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanUsersRepository extends JpaRepository<PlanUsers, Long> {
    List<PlanUsers> findAllByUserId(UUID userId);
}
