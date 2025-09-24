package com.pravell.expense.domain.repository;

import com.pravell.expense.domain.model.Expense;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
}
