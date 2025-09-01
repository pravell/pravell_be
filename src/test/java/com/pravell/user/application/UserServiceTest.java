package com.pravell.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.pravell.common.exception.InvalidCredentialsException;
import com.pravell.user.application.dto.request.SignUpApplicationRequest;
import com.pravell.user.domain.event.UserCreatedEvent;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DisplayName("유저 저장에 성공하면 UserCreatedEvent를 반환한다.")
    @Test
    void shouldReturnUserCreatedEvent_whenUserIsSavedSuccessfully() {
        //given
        SignUpApplicationRequest request = SignUpApplicationRequest.builder()
                .id("testId")
                .password("testPassword")
                .nickname("nickname")
                .build();

        //when
        UserCreatedEvent userCreatedEvent = userService.persistUser(request);

        //then
        Optional<User> savedUser = userRepository.findByUserId(request.getId());
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getPassword()).isNotEqualTo(request.getPassword());

        assertThat(userCreatedEvent.getUser().getId()).isEqualTo(savedUser.get().getId());
        assertThat(userCreatedEvent.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userCreatedEvent.getUser().getUserId()).isEqualTo(request.getId());
        assertThat(userCreatedEvent.getUser().getUserId()).isEqualTo(savedUser.get().getUserId());
    }

    @DisplayName("비밀번호가 인코딩된 비밀번호와 일치하면 검증에 성공한다.")
    @Test
    void shouldValidatePassword_whenMatchesEncodedPassword() {
        //given
        String password = "testPasswordd";
        String encodePassword = passwordEncoder.encode(password);

        //when, then
        assertDoesNotThrow(() -> userService.verifyPassword(password, encodePassword));
    }

    @DisplayName("비밀번호가 인코딩된 비밀번호와 일치하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenPasswordDoesNotMatchEncodedPassword() {
        //given
        String password = "testPasswordd";
        String encodePassword = passwordEncoder.encode("passwordddd");

        //when, then
        assertThatThrownBy(()->userService.verifyPassword(password, encodePassword))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

}
