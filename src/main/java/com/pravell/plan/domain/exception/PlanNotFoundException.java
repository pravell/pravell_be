package com.pravell.plan.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class PlanNotFoundException extends NotFoundException {
    public PlanNotFoundException(String message) {
        super(message);
    }
}
