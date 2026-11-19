package dongsim.dadok.auth;

import dongsim.dadok.auth.domain.dto.TokenResponse;
import dongsim.dadok.auth.domain.entity.RefreshToken;
import dongsim.dadok.auth.domain.exception.AuthenticationFailedException;
import dongsim.dadok.auth.domain.jwt.JwtTokenProvider;
import dongsim.dadok.auth.domain.repository.RefreshTokenRepository;
import dongsim.dadok.auth.domain.repository.UserRepository;
import dongsim.dadok.auth.domain.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// refresh 토큰 재발급: 교체, 유예 시간 안의 재시도, 유예 뒤 재사용, 다른 기기의 예전 토큰, 해시 저장 전 행
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceRefreshTest {

    private static final String USER = "reader01";

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @InjectMocks private AuthService authService;

    private int issued;

    @BeforeEach
    void setUp() {
        issued = 0;
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(USER);
        when(jwtTokenProvider.createAccessToken(USER)).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(USER)).thenAnswer(inv -> "refresh-" + (++issued));
    }

    private RefreshToken stored(RefreshToken token) {
        when(refreshTokenRepository.findByUserIdForUpdate(USER)).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    @DisplayName("지금 토큰이면 새 토큰으로 교체하고, DB에는 원문이 아닌 해시를 저장한다")
    void rotate() {
        RefreshToken saved = stored(new RefreshToken(null, USER, "old"));

        TokenResponse res = authService.refresh("old");

        assertThat(res.refreshToken()).isEqualTo("refresh-1");
        assertThat(saved.matches("refresh-1")).isTrue();
        assertThat(saved.matches("old")).isFalse();
        assertThat(saved.getToken()).hasSize(64).isNotEqualTo("refresh-1");
    }

    @Test
    @DisplayName("교체 직후 바로 전 토큰으로 다시 오면 다시 교체하지 않고 같은 새 토큰을 준다 (응답을 못 받은 재시도)")
    void retryWithinGraceGetsSameCookie() {
        RefreshToken saved = stored(new RefreshToken(null, USER, "old"));
        TokenResponse first = authService.refresh("old");

        TokenResponse retry = authService.refresh("old");

        assertThat(retry.refreshToken()).isEqualTo(first.refreshToken());
        assertThat(saved.matches(first.refreshToken())).isTrue();
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("유예 시간이 지난 뒤 바로 전 토큰이 다시 오면 탈취로 보고 저장된 토큰을 지운다")
    void reuseAfterGraceRevokes() {
        RefreshToken saved = new RefreshToken(null, USER, "old");
        saved.rotate("new", LocalDateTime.now().minusSeconds(30));
        stored(saved);

        assertThatThrownBy(() -> authService.refresh("old")).isInstanceOf(AuthenticationFailedException.class);
        verify(refreshTokenRepository).delete(saved);
    }

    @Test
    @DisplayName("교체 기록과 상관없는 예전 토큰(다른 기기)은 거절만 하고 지금 로그인은 지우지 않는다")
    void unrelatedOldTokenKeepsSession() {
        RefreshToken saved = stored(new RefreshToken(null, USER, "current"));

        assertThatThrownBy(() -> authService.refresh("other-device")).isInstanceOf(AuthenticationFailedException.class);
        verify(refreshTokenRepository, never()).delete(any());
        assertThat(saved.matches("current")).isTrue();
    }

    @Test
    @DisplayName("해시 저장 전에 원문으로 저장된 행도 재발급되고, 그 뒤에는 해시로 저장된다")
    void legacyRawRowStillWorks() {
        // 해시 저장 전 버전이 남긴 행: token 컬럼에 원문이 들어 있다
        RefreshToken real = new RefreshToken(null, USER, "x");
        ReflectionTestUtils.setField(real, "token", "legacy-raw");
        stored(real);

        TokenResponse res = authService.refresh("legacy-raw");

        assertThat(res.refreshToken()).isEqualTo("refresh-1");
        assertThat(real.matches("refresh-1")).isTrue();
        assertThat(real.getToken()).hasSize(64);
    }
}
