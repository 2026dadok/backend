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

    @Id
    @ID
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "lib_book_id", nullable = false)
    private Long libBookId;
    private Long libBookID;

    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;

    @Column(name = "review", columnDefinition = "TEXT", nullable = false)
    private String review;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Builder
    public Review(Long libBookId, LocalDateTime reviewDate, String review) {
        this.libBookId = libBookId;
    public Review(Long libBookID, LocalDateTime reviewDate, String review) {
        this.libBookID = libBookID;
        this.reviewDate = (reviewDate != null) ? reviewDate : LocalDateTime.now();
        this.review = review;
    }

    public void updateReview(String review) {
        this.review = review;
    }
}