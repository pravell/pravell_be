package com.pravell.route.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class RouteNotFoundException extends NotFoundException {
    public RouteNotFoundException(String message) {
        super(message);
    }
}
