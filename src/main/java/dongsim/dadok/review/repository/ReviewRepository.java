package dongsim.dadok.review.repository;

import dongsim.dadok.review.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
<<<<<<< HEAD
    List<Review> findAllByLibBookIdOrderByReviewDateAsc(Long libBookId);
=======
    List<Review> findAllBylibBookIdOrderByReviewDateAsc(Long libBookId);
>>>>>>> fc78119 (feat: dadok 전체 코드 커밋)
}