package com.pravell.marker.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class MarkerNotFoundException extends NotFoundException {
    public MarkerNotFoundException(String message) {
        super(message);
    }
}
