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
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    // 아이디(이메일) 중복 확인
    @GetMapping("/check_id")
    public ResponseEntity<Boolean> checkId(@RequestParam String email) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    // 아이디 찾기
    @PostMapping("/find_id")
    public ResponseEntity<String> findId(@Valid @RequestBody FindIdRequest request) {
        String email = authService.findId(request);
        return ResponseEntity.ok("찾으신 아이디(이메일)는: " + email + " 입니다.");
    }

    // 비밀번호 재설정
    @PostMapping("/pw_reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenResponse tokens = authService.login(request);

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(14 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok(tokens);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @AuthenticationPrincipal String email,
            HttpServletResponse response) {

        authService.logout(email);

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