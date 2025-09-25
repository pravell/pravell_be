package com.pravell.expense.application;

import com.pravell.expense.domain.exception.ExpenseNotFoundException;
import com.pravell.expense.domain.model.Expense;
import com.pravell.expense.domain.repository.ExpenseRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public Expense findExpense(UUID expenseId){
        Optional<Expense> expense = expenseRepository.findById(expenseId);

        if (expense.isEmpty() || expense.get().isDeleted()){
            throw new ExpenseNotFoundException("지출 내역을 찾을 수 없습니다.");
        }

        return expense.get();
    }

}
