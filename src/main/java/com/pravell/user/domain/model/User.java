package com.pravell.user.domain.model;

import com.pravell.common.domain.AggregateRoot;
import com.pravell.user.domain.event.UserCreatedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends AggregateRoot {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public static UserCreatedEvent createUser(String userId, String password, String nickname){
        User user = User.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .password(password)
                .nickname(nickname)
                .status(UserStatus.ACTIVE)
                .build();

        return new UserCreatedEvent(user, LocalDateTime.now());
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
