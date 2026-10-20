package dongsim.dadok.auth.domain.controller;

import dongsim.dadok.auth.domain.dto.*;
import dongsim.dadok.auth.domain.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 이메일 인증번호 발송 (SIGNUP / FIND_ID / FIND_PASSWORD)
    @PostMapping("/email/send")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody EmailSendRequest request) {
        authService.sendVerificationCode(request);
        return ResponseEntity.ok("인증 코드가 이메일로 발송되었습니다.");
    }

    // 이메일 인증번호 확인
    @PostMapping("/email/verify")
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

    // 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<String> findId(@Valid @RequestBody FindIdRequest request) {
        String email = authService.findId(request);
        return ResponseEntity.ok("찾으신 아이디(이메일)는: " + email + " 입니다.");
    }

    // 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokens = authService.login(request);

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)            // JavaScript 접근 불가 (XSS 방어)
                .secure(true)              // HTTPS 환경에서만 전송 (운영 환경 권장)
                .path("/")                 // 모든 경로에서 쿠키 전송
                .maxAge(14 * 24 * 60 * 60) // 14일 (초 단위)
                .sameSite("Lax")           // CSRF 완화 (Lax 또는 Strict)
                .build();

        // Response Header에 Set-Cookie 추가
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // Access Token만 Response Body로 전달 (또는 Access Token도 쿠키로 전송 가능)
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal String email,
            HttpServletResponse response) {

        authService.logout(email);

        // Max-Age를 0으로 설정하여 쿠키 즉시 만료 삭제
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

}