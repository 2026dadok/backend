package dongsim.dadok.quest.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// 퀘스트 목록 (고정 데이터: 일일 출석하기, 30분 독서하기 ...)
@Entity
@Table(name = "quests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String title;           // 화면에 보이는 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestPeriod period;     // DAILY / WEEKLY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestCondition conditionType;

    @Column(nullable = false)
    private Integer targetValue;    // 목표값 (30분, 120분, 300페이지 ...)

    @Column(nullable = false)
    private Integer rewardXp;       // 완료 시 받는 XP
}
