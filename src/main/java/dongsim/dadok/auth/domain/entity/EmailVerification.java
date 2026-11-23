package dongsim.dadok.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verification type;

    @Column(nullable = false)
    private Boolean isVerified;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 인증번호 틀린 횟수 (null 이면 0)
    private Integer attempts;

    public void verify() {
        this.isVerified = true;
    }

    public static final int MAX_ATTEMPTS = 5;

    public void recordFailure() {
        this.attempts = (attempts == null ? 0 : attempts) + 1;
    }

    public boolean isLocked() {
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }
}