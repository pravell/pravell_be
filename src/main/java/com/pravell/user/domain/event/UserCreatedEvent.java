package com.pravell.user.domain.event;

import com.pravell.user.domain.model.User;
import java.time.ZonedDateTime;

public class UserCreatedEvent extends UserEvent {

    public UserCreatedEvent(User user, ZonedDateTime createdAt) {
        super(user, createdAt);
    }

}
