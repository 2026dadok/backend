package dongsim.dadok.review.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "book_review_manage")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "lib_book_id", nullable = false)
    private Long libBookId;

    // 작성자 (users.id). 이 컬럼이 생기기 전에 쓴 감상문은 비어 있을 수 있어 nullable 로 둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User writer;

    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;

    @Column(name = "review", columnDefinition = "TEXT", nullable = false)
    private String review;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Builder
    public Review(Long libBookId, User writer, LocalDateTime reviewDate, String review) {
        this.libBookId = libBookId;
        this.writer = writer;
        this.reviewDate = (reviewDate != null) ? reviewDate : LocalDateTime.now();
        this.review = review;
    }

    public void updateReview(String review) {
        this.review = review;
    }
}
