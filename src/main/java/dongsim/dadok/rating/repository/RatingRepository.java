package dongsim.dadok.rating.repository;

import dongsim.dadok.rating.domain.entity.Rating;
import dongsim.dadok.rating.domain.projection.RatingSummaryProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByBookBookIdAndUserId(Long bookId, String userId);

    Page<Rating> findByBookBookId(Long bookId, Pageable pageable);

    @Query("""
            select avg(r.ratingScore) as averageRating, count(r) as reviewCount
            from Rating r
            where r.book.bookId = :bookId
            """)
    RatingSummaryProjection getSummaryByBookId(@Param("bookId") Long bookId);
}
