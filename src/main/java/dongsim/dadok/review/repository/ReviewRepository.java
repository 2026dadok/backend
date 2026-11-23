package dongsim.dadok.review.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.review.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByLibBookIdOrderByReviewDateAsc(Long libBookId);

    long countByWriter(User writer);
}