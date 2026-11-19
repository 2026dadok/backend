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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

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

        // 재발송은 1분에 한 번 (유효시간 5분이므로 만료까지 4분 넘게 남았으면 1분 안에 보낸 것)
        verificationRepository.findTopByEmailAndTypeOrderByExpiresAtDesc(request.email(), request.purpose())
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(4)))
                .ifPresent(v -> {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 뒤에 다시 받을 수 있습니다.");
                });

        // 기존 인증 기록 삭제 (무결성 보장)
        verificationRepository.deleteByEmailAndType(request.email(), request.purpose());

        String code = String.format("%06d", RANDOM.nextInt(1000000));
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
    // 틀린 횟수는 예외가 나도 저장되어야 하므로 롤백하지 않는다
    @Transactional(noRollbackFor = InvalidVerificationException.class)
    public boolean verifyCode(EmailVerifyRequest request) {
        EmailVerification verification = checkCode(request.email(), request.code(), request.purpose());
        verification.verify();
        verificationRepository.saveAndFlush(verification);
        return true;
    }

    // 가장 최근 인증번호와 비교 (만료, 시도 횟수 초과, 불일치 검사)
    private EmailVerification checkCode(String email, String code, Verification type) {
        EmailVerification verification = verificationRepository
                .findFirstByEmailAndTypeOrderByExpiresAtDesc(email, type)
                .orElseThrow(() -> new InvalidVerificationException("인증번호가 일치하지 않거나 존재하지 않습니다."));

        if (verification.isLocked()) {
            throw new InvalidVerificationException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 받아주세요.");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationException("만료된 인증번호입니다.");
        }
        if (code == null || !verification.getCode().equals(code)) {
            verification.recordFailure();
            verificationRepository.saveAndFlush(verification);
            throw new InvalidVerificationException("인증번호가 일치하지 않거나 존재하지 않습니다.");
        }
        return verification;
    }

    // 3. 회원가입 (이메일 인증 필수 검증 포함)
    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByUserId(request.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }

        EmailVerification verification = verificationRepository
                .findVerifiedLatest(request.email(), Verification.SIGNUP)
                .orElseThrow(() -> new InvalidVerificationException("이메일 인증이 필요합니다."));
        checkVerifiedRecent(verification);

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
        verificationRepository.delete(verification); // 인증 기록은 한 번만 쓴다
    }

    // 4. 로그인
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUserId(request.userId())
                .orElseThrow(() -> new AuthenticationFailedException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationFailedException("아이디 또는 비밀번호가 올바르지 않습니다.");
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

    // 토큰 재발급: 저장된 refresh 토큰과 같을 때만 새 access/refresh 토큰 발급 (refresh 토큰도 교체)
    // 예전 토큰 재사용으로 저장된 토큰을 지운 뒤 예외를 던지므로 롤백하지 않는다
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new AuthenticationFailedException("다시 로그인해주세요.");
        }
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        RefreshToken saved = refreshTokenRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationFailedException("다시 로그인해주세요."));
        LocalDateTime now = LocalDateTime.now();
        if (!refreshToken.equals(saved.getToken())) {
            // 방금 교체된 토큰(동시 재발급, 응답을 못 받은 재시도)이면 지금 토큰을 다시 준다
            if (saved.isJustRotated(refreshToken, now)) {
                return new TokenResponse(jwtTokenProvider.createAccessToken(userId), saved.getToken());
            }
            // 오래전에 교체된 예전 토큰이 다시 왔다 = 탈취 가능성 → 저장된 토큰도 지워서 모두 다시 로그인하게 한다
            refreshTokenRepository.delete(saved);
            throw new AuthenticationFailedException("다시 로그인해주세요.");
        }

        String newAccess = jwtTokenProvider.createAccessToken(userId);
        String newRefresh = jwtTokenProvider.createRefreshToken(userId);
        saved.rotate(newRefresh, now);
        return new TokenResponse(newAccess, newRefresh);
    }

    // 5. 로그아웃
    @Transactional
    public void logout(String userId, String refreshToken) {
        String target = userId;
        if ((target == null || "anonymousUser".equals(target)) && refreshToken != null
                && jwtTokenProvider.validateToken(refreshToken) && jwtTokenProvider.isRefreshToken(refreshToken)) {
            target = jwtTokenProvider.getUserIdFromToken(refreshToken);
        }
        if (target != null && !"anonymousUser".equals(target)) {
            refreshTokenRepository.deleteByUserId(target);
        }
    }

    // 6. 아이디 찾기 (이름, 생년월일, 인증완료된 이메일 정보 확인)
    // findVerifiedLatest 가 잠금 조회(FOR UPDATE)라서 읽기 전용 트랜잭션이면 안 된다
    @Transactional
    public String findId(FindIdRequest request) {
        EmailVerification verification = verificationRepository
                .findVerifiedLatest(request.email(), Verification.FIND_ID)
                .orElseThrow(() -> new InvalidVerificationException("인증되지 않았거나 인증 내역이 존재하지 않습니다."));
        checkVerifiedRecent(verification);

        User user = userRepository.findByNameAndBirthDateAndEmail(
                        request.name(), request.birthDate(), request.email())
                .orElseThrow(() -> new UserNotFoundException("일치하는 회원 정보를 찾을 수 없습니다."));
        verificationRepository.delete(verification); // 인증 기록은 한 번만 쓴다

        return user.getUserId();
    }

    // 7. 비밀번호 재설정 (이메일 인증 완료 + 같은 인증번호, 사용 후 폐기)
    @Transactional(noRollbackFor = InvalidVerificationException.class)
    public void resetPassword(ResetPasswordRequest request) {
        EmailVerification verification = checkCode(request.email(), request.code(), Verification.FIND_PASSWORD);
        if (!Boolean.TRUE.equals(verification.getIsVerified())) {
            throw new InvalidVerificationException("이메일 인증을 먼저 완료해주세요.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        verificationRepository.delete(verification);
        refreshTokenRepository.deleteByUserId(user.getUserId()); // 다른 기기의 로그인도 끊는다
    }

    // 인증 완료 후 30분 안에만 가입·아이디 찾기에 쓸 수 있다 (인증번호 유효 5분 + 25분)
    private void checkVerifiedRecent(EmailVerification verification) {
        if (verification.getExpiresAt().plusMinutes(25).isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationException("인증 시간이 지났습니다. 이메일 인증을 다시 해주세요.");
        }
    }

    // 8. 이메일 중복 확인
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}

