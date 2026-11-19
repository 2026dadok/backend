package dongsim.dadok.auth.domain.service;


import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
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

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthService.class);

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
        // 사용자 행을 잠근 상태로 읽어서 비밀번호를 확인한다
        // (비밀번호 재설정·변경과 겹치면 한쪽이 끝난 뒤의 비밀번호로 확인하게 되어, 재설정 뒤에 예전 비밀번호로 세션이 생기지 않는다)
        User user = userRepository.findByUserIdForUpdate(request.userId())
                .orElseThrow(() -> new AuthenticationFailedException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationFailedException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // 재발급과 겹치지 않게 잠그고 바꾼다 (사용자 행은 위에서 이미 잠가서 첫 로그인이 동시에 와도 한 번만 만든다)
        refreshTokenRepository.findByUserIdForUpdate(user.getUserId())
                .ifPresentOrElse(
                        token -> token.updateToken(refreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(null, user.getUserId(), refreshToken))
                );

        return new TokenResponse(accessToken, refreshToken);
    }

    // 토큰 재발급: 저장된 refresh 토큰과 같을 때만 새 access/refresh 토큰 발급 (refresh 토큰도 교체)
    // 교체 직후(10초 이내) 바로 전 토큰이 오면 access 토큰만 새로 주고 refresh 토큰은 그대로 둔다 (응답의 refreshToken 은 null)
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
        if (!saved.matches(refreshToken)) {
            // 방금 교체된 토큰(동시 재발급)이면 access 토큰만 새로 준다
            if (saved.isJustRotated(refreshToken, now)) {
                // 응답을 못 받은 재시도일 수 있으므로, 기억해 둔 지금 토큰이 있으면 같은 쿠키를 다시 준다 (다시 교체하지 않음)
                String current = recentRefreshToken(saved);
                LOG.info("교체 직후 이전 refresh 토큰으로 재발급 userId={} 쿠키 재전달={}", userId, current != null);
                return new TokenResponse(jwtTokenProvider.createAccessToken(userId), current);
            }
            // 재발급으로 이미 교체된 바로 전 토큰이 유예 뒤에 다시 왔다 = 탈취 가능성 → 저장된 토큰도 지운다
            if (saved.isPrevious(refreshToken)) {
                refreshTokenRepository.delete(saved);
            }
            // 그 밖의 예전 토큰(다시 로그인하기 전 다른 기기의 토큰 등)은 거절만 하고 지금 로그인은 건드리지 않는다
            throw new AuthenticationFailedException("다시 로그인해주세요.");
        }

        String newAccess = jwtTokenProvider.createAccessToken(userId);
        String newRefresh = jwtTokenProvider.createRefreshToken(userId);
        saved.rotate(newRefresh, now);
        rememberAfterCommit(userId, newRefresh, now);
        return new TokenResponse(newAccess, newRefresh);
    }

    // 재발급 직후의 새 refresh 토큰 원문을 잠깐(1분) 서버 메모리에만 기억한다.
    // DB에는 해시만 있어서, 유예 시간 안의 재시도에 같은 쿠키를 다시 주려면 원문이 필요하다.
    // 서버가 재시작되면 비워지고, 그때 재시도는 access 토큰만 받는다
    private record RecentRefresh(String rawToken, LocalDateTime issuedAt) {}
    private final Map<String, RecentRefresh> recentRefresh = new ConcurrentHashMap<>();

    private String recentRefreshToken(RefreshToken saved) {
        RecentRefresh recent = recentRefresh.get(saved.getUserId());
        return recent != null && saved.matches(recent.rawToken()) ? recent.rawToken() : null;
    }

    // 교체가 커밋된 뒤에만 기억한다 (롤백되면 기억하지 않음)
    private void rememberAfterCommit(String userId, String rawToken, LocalDateTime now) {
        Runnable remember = () -> {
            recentRefresh.values().removeIf(r -> r.issuedAt().isBefore(now.minusMinutes(1)));
            recentRefresh.put(userId, new RecentRefresh(rawToken, now));
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    remember.run();
                }
            });
        } else {
            remember.run();
        }
    }

    // 5. 로그아웃
    @Transactional
    public void logout(String userId, String refreshToken) {
        String target = userId;
        if (target != null && !"anonymousUser".equals(target)) {
            refreshTokenRepository.deleteByUserId(target);
            return;
        }
        // access 토큰이 만료된 로그아웃: refresh 쿠키가 지금 저장된 토큰과 같을 때만 지운다
        // (예전 토큰으로 다른 기기의 새 로그인을 끊지 않게)
        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken) && jwtTokenProvider.isRefreshToken(refreshToken)) {
            String owner = jwtTokenProvider.getUserIdFromToken(refreshToken);
            refreshTokenRepository.findByUserIdForUpdate(owner)
                    .filter(saved -> saved.matches(refreshToken))
                    .ifPresent(refreshTokenRepository::delete);
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

        // 로그인과 같은 사용자 행 잠금: 재설정이 끝나기 전 로그인은 기다렸다가 새 비밀번호로 확인된다
        User user = userRepository.findByEmailForUpdate(request.email())
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

