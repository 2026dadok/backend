package dongsim.dadok.league.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.Tier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 주간 경쟁 참가 기록: 유저 1명의 한 주
// 리그(티어)는 그 주 처음 참가할 때 고정되고, 정산 때 순위·보상이 채워짐
@Entity
@Table(name = "weekly_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}),
        indexes = @Index(columnList = "week_start, tier"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;        // 그 주 월요일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tier tier;                  // 이번 주 리그

    private Integer finalRank;          // 최종 순위 (정산 전 / 0분이면 null)

    private Long totalSeconds;          // 최종 독서 시간 (정산 후 채워짐)

    private Integer rewardXp;           // 정산 보상 XP

    @Column(nullable = false)
    private boolean settled = false;    // 정산 완료 여부

    @Column(nullable = false)
    private boolean resultSeen = false; // 결과 알림을 봤는지

    public WeeklyEntry(User user, LocalDate weekStart, Tier tier) {
        this.user = user;
        this.weekStart = weekStart;
        this.tier = tier;
    }

    public void settle(Integer finalRank, long totalSeconds, int rewardXp) {
        this.finalRank = finalRank;
        this.totalSeconds = totalSeconds;
        this.rewardXp = rewardXp;
        this.settled = true;
    }

    public void markResultSeen() {
        this.resultSeen = true;
    }
}
