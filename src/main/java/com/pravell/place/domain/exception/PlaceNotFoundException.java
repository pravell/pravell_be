package com.pravell.place.domain.exception;

import com.pravell.common.exception.NotFoundException;

public class PlaceNotFoundException extends NotFoundException {
    public PlaceNotFoundException(String message) {
        super(message);
    }
}
