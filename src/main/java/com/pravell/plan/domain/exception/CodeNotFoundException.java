package com.pravell.plan.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class CodeNotFoundException extends NotFoundException {
    public CodeNotFoundException(String message) {
        super(message);
    }
}
