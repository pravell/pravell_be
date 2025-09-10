package com.pravell.route.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class RoutePlaceNotFoundException extends NotFoundException {
    public RoutePlaceNotFoundException(String message) {
        super(message);
    }
}
