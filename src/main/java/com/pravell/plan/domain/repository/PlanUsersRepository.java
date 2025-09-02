package com.pravell.plan.domain.repository;

import com.pravell.plan.domain.model.PlanUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanUsersRepository extends JpaRepository<PlanUsers, Long> {
}
