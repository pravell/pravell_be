package com.pravell.plan.domain.repository;

import com.pravell.plan.domain.model.PlanInviteCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanInviteCodeRepository extends JpaRepository<PlanInviteCode, Long> {
    Optional<PlanInviteCode> findByCode(String code);
}
