package com.pravell.expense.domain.repository;

import com.pravell.expense.domain.model.Expense;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("""
                select e
                from Expense e
                where e.planId = :planId
                  and e.isDeleted = false
                  and (:from is null or e.spentAt >= :from)
                  and (:to   is null or e.spentAt <  :to)
                  and (:paidByUserId is null or e.paidByUserId = :paidByUserId)
                order by e.spentAt desc
            """)
    List<Expense> findAllByPlanIdWithFilters(@Param("planId") UUID planId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("paidByUserId") UUID paidByUserId);

}
