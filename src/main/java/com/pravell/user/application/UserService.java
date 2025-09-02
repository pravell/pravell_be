package com.pravell.user.application;

import com.pravell.common.exception.InvalidCredentialsException;
import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import com.pravell.user.application.dto.response.UserProfileResponse;
import com.pravell.user.domain.event.UserCreatedEvent;
import com.pravell.user.domain.exception.UserNotFoundException;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserCreatedEvent persistUser(SignUpApplicationRequest request) {
        validSignUp(request);

        String encodePassword = passwordEncoder.encode(request.getPassword());

        UserCreatedEvent userCreatedEvent = User.createUser(request.getId(), encodePassword, request.getNickname());
        userRepository.save(userCreatedEvent.getUser());

        return userCreatedEvent;
    }

    private void validSignUp(SignUpApplicationRequest signUpApplicationRequest) {
        boolean existsById = userRepository.existsByUserId(signUpApplicationRequest.getId());
        if (existsById) {
            throw new DuplicateKeyException("이미 존재하는 아이디입니다.");
        }

        boolean existsByNickname = userRepository.existsByNickname(signUpApplicationRequest.getNickname());
        if (existsByNickname) {
            throw new DuplicateKeyException("이미 존재하는 닉네임입니다.");
        }

    }

    @Transactional(readOnly = true)
    public User findUserByUserId(String id) {
        Optional<User> user = userRepository.findByUserId(id);

        if (user.isEmpty()) {
            log.warn("유저를 찾을 수 없습니다. Id : {}", id);
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        return user.get();
    }

    @Transactional(readOnly = true)
    public User findUserById(UUID id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            log.warn("유저를 찾을 수 없습니다. Id : {}", id);
            throw new UserNotFoundException("유저를 찾을 수 없습니다.");
        }

        return user.get();
    }

    public void verifyPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidCredentialsException("비밀번호가 일치하지 않습니다.");
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID id) {
        User user = findUserById(id);

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .build();
    }

}
