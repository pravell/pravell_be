package com.pravell.expense.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class ExpenseNotFoundException extends NotFoundException {
    public ExpenseNotFoundException(String message) {
        super(message);
    }
}
