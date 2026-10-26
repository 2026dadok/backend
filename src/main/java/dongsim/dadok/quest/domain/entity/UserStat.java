package dongsim.dadok.quest.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

// 유저별 게임 기록 (XP, 사용 중인 칭호)
@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저 1명 : 기록 1개 → @OneToOne
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer xp = 0;                 // 누적 XP (티어 계산용, 줄어들지 않음)

    private Long equippedAchievementId;     // 사용 중인 칭호 (없으면 null)

    public UserStat(User user) {
        this.user = user;
        this.xp = 0;
    }

    public void addXp(int amount) {
        this.xp += amount;
    }
}
