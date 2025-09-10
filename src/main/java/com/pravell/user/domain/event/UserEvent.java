package com.pravell.user.domain.event;

import com.pravell.common.domain.event.DomainEvent;
import com.pravell.user.domain.model.User;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public abstract class UserEvent implements DomainEvent<User> {

    private final User user;
    private final LocalDateTime createdAt;

    public UserEvent(User user, LocalDateTime createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

}
