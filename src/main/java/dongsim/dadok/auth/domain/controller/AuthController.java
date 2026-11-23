package dongsim.dadok.auth.domain.controller;

import dongsim.dadok.auth.domain.dto.*;
import dongsim.dadok.auth.domain.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // refresh 쿠키 Secure 여부. https 배포에서는 true, http 로 개발할 때는 false (http 에서는 Secure 쿠키가 저장되지 않음)
    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    // 이메일 인증번호 발송
    @PostMapping("/email_send")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody EmailSendRequest request) {
        authService.sendVerificationCode(request);
        return ResponseEntity.ok("인증 코드가 이메일로 발송되었습니다.");
    }

    // 이메일 인증번호 확인
    @PostMapping("/email_verify")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody EmailVerifyRequest request) {
        authService.verifyCode(request);
        return ResponseEntity.ok("이메일 인증이 성공적으로 완료되었습니다.");
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    // 아이디 중복 확인
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkIdDuplicate(@RequestParam String userId) {
        boolean isDuplicate = authService.existsByUserId(userId);
        return ResponseEntity.ok(isDuplicate);
    }
    // 아이디 찾기
    @PostMapping("/find_id")
    public ResponseEntity<String> findId(@Valid @RequestBody FindIdRequest request) {
        String userId = authService.findId(request);
        return ResponseEntity.ok("찾으신 아이디는: " + userId + " 입니다.");
    }

    // 비밀번호 재설정
    @PostMapping("/pw_reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokens = authService.login(request);

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // refresh 토큰은 httpOnly 쿠키로만 주고 본문에는 access 토큰만 넣는다
        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    // 토큰 재발급: refreshToken 쿠키(또는 본문 refreshToken)로 새 accessToken 발급
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(value = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response) {

        String refreshToken = cookieToken != null ? cookieToken : (body == null ? null : body.get("refreshToken"));
        TokenResponse tokens = authService.refresh(refreshToken);

        // 교체 직후 재요청이면 refresh 토큰은 그대로라 쿠키를 다시 주지 않는다
        if (tokens.refreshToken() != null) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                    .httpOnly(true)
                    .secure(secureCookie)
                    .path("/")
                    .maxAge(14 * 24 * 60 * 60)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }

        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal String userId,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        // access 토큰이 만료돼도 refresh 쿠키로 사용자를 찾아 서버의 refresh 토큰을 지운다
        authService.logout(userId, refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}