package dongsim.dadok.auth;

import dongsim.dadok.auth.domain.dto.SignUpRequest;
import dongsim.dadok.auth.domain.entity.EmailVerification;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.auth.domain.entity.Verification;
import dongsim.dadok.auth.domain.exception.InvalidVerificationException;
import dongsim.dadok.auth.domain.repository.EmailVerificationRepository;
import dongsim.dadok.auth.domain.repository.UserRepository;
import dongsim.dadok.auth.domain.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationRepository verificationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - 인증된 이메일 사용")
    void signUpSuccess() {
        // given
        SignUpRequest request = new SignUpRequest("testuser", "test@test.com", "pw123!", "이름", LocalDate.now(), 1800);
        when(userRepository.existsByUserId("testuser")).thenReturn(false);
        when(verificationRepository.findVerifiedLatest("test@test.com", Verification.SIGNUP))
                .thenReturn(Optional.of(mock(EmailVerification.class)));
        when(passwordEncoder.encode("pw123!")).thenReturn("encodedPassword");

        // when
        authService.signUp(request);

        // then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 안 됨")
    void signUpFail_NoVerification() {
        // given
        SignUpRequest request = new SignUpRequest("testuser", "test@test.com", "pw123!", "이름", LocalDate.now(), 1800);
        when(userRepository.existsByUserId("testuser")).thenReturn(false);
        when(verificationRepository.findVerifiedLatest("test@test.com", Verification.SIGNUP))
                .thenReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(InvalidVerificationException.class)
                .hasMessage("이메일 인증이 필요합니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    void signUpFail_DuplicateUserId() {
        // given
        SignUpRequest request = new SignUpRequest("testuser", "test@test.com", "pw123!", "이름", LocalDate.now(), 1800);
        when(userRepository.existsByUserId("testuser")).thenReturn(true);

        // then
        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
    }
}