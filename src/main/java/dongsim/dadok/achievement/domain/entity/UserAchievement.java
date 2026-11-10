package dongsim.dadok.achievement.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// 유저별 업적 달성 기록 (같은 업적은 한 번만)
@Entity
@Table(name = "user_achievements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(nullable = false)
    private LocalDateTime achievedAt;

    public UserAchievement(User user, Achievement achievement, LocalDateTime achievedAt) {
        this.user = user;
        this.achievement = achievement;
        this.achievedAt = achievedAt;
    }
}
