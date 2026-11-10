package dongsim.dadok.rating.domain.entity;

import dongsim.dadok.book.domain.entity.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

@Entity
@Table(
        name = "rating",
        uniqueConstraints = @UniqueConstraint(name = "uk_rating_book_user", columnNames = {"book_id", "user_id"})
)
@Check(constraints = "rating_score BETWEEN 1 AND 5")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long ratingId;

    @Column(name = "rating_score", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private int ratingScore;

    @Column(name = "rating_review", nullable = false, length = 255)
    private String ratingReview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    protected Rating() {
    }

    public Rating(Book book, String userId, int ratingScore, String ratingReview) {
        this.book = book;
        this.userId = userId;
        this.ratingScore = ratingScore;
        this.ratingReview = ratingReview;
    }

    public Long getRatingId() {
        return ratingId;
    }

    public int getRatingScore() {
        return ratingScore;
    }

    public String getRatingReview() {
        return ratingReview;
    }

    public String getUserId() {
        return userId;
    }

    public void update(int ratingScore, String ratingReview) {
        this.ratingScore = ratingScore;
        this.ratingReview = ratingReview;
    }
}
