package dongsim.dadok.auth.domain.service;


import dongsim.dadok.auth.domain.Role;
import dongsim.dadok.auth.domain.dto.*;
import dongsim.dadok.auth.domain.entity.EmailVerification;
import dongsim.dadok.auth.domain.entity.RefreshToken;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.auth.domain.entity.Verification;
import dongsim.dadok.auth.domain.exception.*;
import dongsim.dadok.auth.domain.jwt.JwtTokenProvider;
import dongsim.dadok.auth.domain.repository.EmailVerificationRepository;
import dongsim.dadok.auth.domain.repository.RefreshTokenRepository;
import dongsim.dadok.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository verificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final jakarta.persistence.EntityManager entityManager; // 추가

    // 아이디 중복 확인
    public boolean existsByUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }

    // 1. 이메일 인증번호 발송 (회원가입/아이디찾기/비번찾기)
    @Transactional
    public void sendVerificationCode(EmailSendRequest request) {
        if (request.purpose() == Verification.SIGNUP && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }
        if ((request.purpose() == Verification.FIND_ID || request.purpose() == Verification.FIND_PASSWORD)
                && !userRepository.existsByEmail(request.email())) {
            throw new UserNotFoundException("존재하지 않는 이메일입니다.");
        }

        // 기존 인증 기록 삭제 (무결성 보장)
        verificationRepository.deleteByEmailAndType(request.email(), request.purpose());

        String code = String.format("%06d", new Random().nextInt(1000000));
        EmailVerification verification = EmailVerification.builder()
                .email(request.email())
                .code(code)
                .type(request.purpose())
                .isVerified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        verificationRepository.save(verification);
        try {
            mailService.sendVerificationCode(request.email(), code);
        } catch (Exception e) {
            System.out.println("========================================");
            System.out.println("[개발/테스트 모드] 인증 코드 생성됨: " + code);
            System.out.println("이메일 전송 실패(SMTP 미설정 등): " + e.getMessage());
            System.out.println("========================================");
        }
    }

    // 2. 이메일 인증번호 검증
    @Transactional
    public boolean verifyCode(EmailVerifyRequest request) {
        EmailVerification verification = verificationRepository
 //               .findByEmailAndCodeAndType(request.email(), request.code(), request.purpose())
                .findByEmailAndCodeAndType(request.email(), request.code(), request.purpose())
                .orElseThrow(() -> new InvalidVerificationException("인증번호가 일치하지 않거나 존재하지 않습니다."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationException("만료된 인증번호입니다.");
        }

        verification.verify();
        verificationRepository.saveAndFlush(verification); // 즉시 반영
        
        // 로그 추가
        System.out.println("DEBUG: 인증 완료됨 - " + verification.getEmail() + ", isVerified: " + verification.getIsVerified());
        
        return true;
    }

    // 3. 회원가입 (이메일 인증 필수 검증 포함)
    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        EmailVerification verification = verificationRepository
                .findVerifiedLatest(request.email(), Verification.SIGNUP)
                .orElseThrow(() -> new InvalidVerificationException("이메일 인증이 필요합니다."));

        User user = User.builder()
                .userId(request.userId())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .birthDate(request.birthDate())
                .defaultTimerTime(request.defaultTimerTime())
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    // 4. 로그인
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUserId(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationFailedException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        refreshTokenRepository.findByUserId(user.getUserId())
                .ifPresentOrElse(
                        token -> token.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(null, user.getUserId(), refreshToken))
                );

        return new TokenResponse(accessToken, refreshToken);
    }

    // 5. 로그아웃
    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    // 6. 아이디 찾기 (이름, 생년월일, 인증완료된 이메일 정보 확인)
    public String findId(FindIdRequest request) {
        EmailVerification verification = verificationRepository
                .findVerifiedLatest(request.email(), Verification.FIND_ID)
                .orElseThrow(() -> new InvalidVerificationException("인증되지 않았거나 인증 내역이 존재하지 않습니다."));

        User user = userRepository.findByNameAndBirthDateAndEmail(
                        request.name(), request.birthDate(), request.email())
                .orElseThrow(() -> new UserNotFoundException("일치하는 회원 정보를 찾을 수 없습니다."));

        return user.getUserId();
    }

    // 7. 비밀번호 재설정 (이메일 인증 후)
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        EmailVerification verification = verificationRepository
                .findByEmailAndCodeAndType(request.email(), request.code(), Verification.FIND_PASSWORD)
                .orElseThrow(() -> new InvalidVerificationException("인증번호가 일치하지 않거나 존재하지 않습니다."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationException("만료된 인증번호입니다.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        
        // 인증 후 처리 (선택사항: 인증 내역 삭제 또는 만료 처리)
        // verificationRepository.delete(verification);
    }

    // 8. 이메일 중복 확인
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}

