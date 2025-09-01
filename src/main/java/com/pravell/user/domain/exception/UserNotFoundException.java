package com.pravell.user.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
