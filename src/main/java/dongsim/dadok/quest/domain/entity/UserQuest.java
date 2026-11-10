package dongsim.dadok.quest.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 유저별 퀘스트 완료 기록
// 같은 유저가 같은 퀘스트를 같은 기간(날짜)에 두 번 완료할 수 없음 → UNIQUE
@Entity
@Table(name = "user_quests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id", "period_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;      // 일일: 그날 / 주간: 그 주 월요일

    @Column(nullable = false)
    private Integer progress;           // 현재 진행값 (예: 180페이지)

    private LocalDateTime completedAt;  // 완료 시각 (미완료면 null)

    // 바로 완료 처리된 기록 만들기 (출석처럼 한 번에 끝나는 퀘스트)
    public static UserQuest completed(User user, Quest quest, LocalDate periodStart) {
        UserQuest uq = new UserQuest();
        uq.user = user;
        uq.quest = quest;
        uq.periodStart = periodStart;
        uq.progress = quest.getTargetValue();
        uq.completedAt = LocalDateTime.now();
        return uq;
    }
}
