package dongsim.dadok.review.domain.entity;

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

<<<<<<< HEAD
    @ID
=======
    @Id
>>>>>>> fc78119 (feat: dadok 전체 코드 커밋)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "lib_book_id", nullable = false)
<<<<<<< HEAD
    private Long libBookID;
=======
    private Long libBookId;
>>>>>>> fc78119 (feat: dadok 전체 코드 커밋)

    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;

    @Column(name = "review", columnDefinition = "TEXT", nullable = false)
    private String review;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Builder
<<<<<<< HEAD
    public Review(Long libBookID, LocalDateTime reviewDate, String review) {
        this.libBookID = libBookID;
=======
    public Review(Long libBookId, LocalDateTime reviewDate, String review) {
        this.libBookId = libBookId;
>>>>>>> fc78119 (feat: dadok 전체 코드 커밋)
        this.reviewDate = (reviewDate != null) ? reviewDate : LocalDateTime.now();
        this.review = review;
    }

    public void updateReview(String review) {
        this.review = review;
    }
}