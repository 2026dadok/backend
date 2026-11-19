package dongsim.dadok.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

// refresh 토큰은 원문 대신 SHA-256 해시(64자)로 저장한다 (컬럼 길이와 무관, DB가 유출돼도 토큰을 쓸 수 없음)
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    // 교체 직후 예전 토큰을 받아 주는 시간 (동시에 온 재발급, 응답을 못 받은 재시도)
    private static final Duration ROTATION_GRACE = Duration.ofSeconds(10);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    // 지금 토큰의 해시
    @Column(nullable = false)
    private String token;

    // 바로 전 토큰의 해시와 교체 시각 (재발급으로 바뀐 경우만)
    @Column(name = "previous_token")
    private String previousToken;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    public RefreshToken(Long id, String userId, String rawToken) {
        this.id = id;
        this.userId = userId;
        this.token = hash(rawToken);
    }

    // 로그인: 새 세션이므로 예전 토큰 기록을 남기지 않는다
    public void updateToken(String rawToken) {
        this.token = hash(rawToken);
        this.previousToken = null;
        this.rotatedAt = null;
    }

    // 재발급: 지금 토큰을 바로 전 토큰으로 남기고 교체한다
    public void rotate(String rawToken, LocalDateTime now) {
        this.previousToken = this.token;
        this.rotatedAt = now;
        this.token = hash(rawToken);
    }

    // 지금 토큰과 같은지
    // 해시 저장 전 버전이 원문으로 저장한 행도 받아 준다 (배포 직후 기존 로그인 유지). 다음 교체부터는 해시로 저장된다
    public boolean matches(String rawToken) {
        return rawToken != null && (token.equals(hash(rawToken)) || token.equals(rawToken));
    }

    // 바로 전 토큰과 같은지 (교체된 시각과 무관)
    public boolean isPrevious(String rawToken) {
        return rawToken != null && previousToken != null && previousToken.equals(hash(rawToken));
    }

    // 방금 교체된 바로 전 토큰인지 (교체 후 10초 이내)
    public boolean isJustRotated(String rawToken, LocalDateTime now) {
        return isPrevious(rawToken) && rotatedAt != null && !now.isAfter(rotatedAt.plus(ROTATION_GRACE));
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 사용할 수 없습니다.", e);
        }
    }
}
