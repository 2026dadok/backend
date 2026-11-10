package dongsim.dadok.achievement.domain.entity;

import jakarta.persistence.*;
import lombok.*;

// 업적 목록 (고정 데이터)
@Entity
@Table(name = "achievements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String title;               // 칭호명

    @Column(nullable = false, length = 100)
    private String description;         // 달성 조건 문구

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AchievementCondition conditionType;

    private Integer targetValue;        // 3권, 100일, 10위 등 (없으면 null)

    @Column(length = 100)
    private String targetBook;          // 특정 도서 업적의 책 제목 (없으면 null)
}
