package com.pravell.user.domain.repository;

import com.pravell.user.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByNickname(String nickname);

    boolean existsByUserId(String id);

    Optional<User> findByUserId(String userId);

}
