package dongsim.dadok.time.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 독서 기록: 타이머로 한 번 읽을 때마다 1줄씩 쌓임
// → 주간 경쟁(이번 주 합계), 30분/2시간 퀘스트(오늘 합계), 캘린더(날짜별 합계)에 사용
@Entity
@Table(name = "reading_logs",
        indexes = @Index(columnList = "user_id, read_date"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // TODO: Book 엔티티 생성 후 @ManyToOne으로 변경
    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "read_date", nullable = false)
    private LocalDate readDate;         // 읽은 날 (한국 시간 기준)

    @Column(nullable = false)
    private Integer seconds;            // 읽은 시간 (초)

    @Column(nullable = false)
    private LocalDateTime endedAt;      // 책 덮은 시각 (주경야독 업적용)

    public ReadingLog(User user, Long bookId, LocalDate readDate, int seconds, LocalDateTime endedAt) {
        this.user = user;
        this.bookId = bookId;
        this.readDate = readDate;
        this.seconds = seconds;
        this.endedAt = endedAt;
    }
}
