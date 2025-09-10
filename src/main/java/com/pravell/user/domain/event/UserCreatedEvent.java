package com.pravell.user.domain.event;

import com.pravell.user.domain.model.User;
import java.time.LocalDateTime;

public class UserCreatedEvent extends UserEvent {

    public UserCreatedEvent(User user, LocalDateTime createdAt) {
        super(user, createdAt);
    }

}
