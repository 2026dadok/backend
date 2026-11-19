package dongsim.dadok.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String token;

    // 바로 전 토큰과 교체 시각 (재발급으로 바뀐 경우만)
    @Column(name = "previous_token")
    private String previousToken;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    public RefreshToken(Long id, String userId, String token) {
        this.id = id;
        this.userId = userId;
        this.token = token;
    }

    // 로그인: 새 세션이므로 예전 토큰 기록을 남기지 않는다
    public void updateToken(String newToken) {
        this.token = newToken;
        this.previousToken = null;
        this.rotatedAt = null;
    }

    // 재발급: 지금 토큰을 바로 전 토큰으로 남기고 교체한다
    public void rotate(String newToken, LocalDateTime now) {
        this.previousToken = this.token;
        this.rotatedAt = now;
        this.token = newToken;
    }

    // 방금 교체된 바로 전 토큰인지 (교체 후 10초 이내)
    public boolean isJustRotated(String presented, LocalDateTime now) {
        return previousToken != null && rotatedAt != null && previousToken.equals(presented)
                && !now.isAfter(rotatedAt.plus(ROTATION_GRACE));
    }
}
