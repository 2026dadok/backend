package dongsim.dadok.review.repository;

import dongsim.dadok.review.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllBylibBookIdOrderByReviewDateAsc(Long libBookId);
}