package dongsim.dadok.rating.service;

import dongsim.dadok.rating.domain.dto.*;
import dongsim.dadok.rating.domain.entity.Rating;
import dongsim.dadok.rating.domain.projection.RatingSummaryProjection;
import dongsim.dadok.rating.repository.RatingRepository;

import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RatingService {

    private static final int PAGE_SIZE = 5;

    private final RatingRepository ratingRepository;
    private final BookRepository bookRepository;

    public RatingService(RatingRepository ratingRepository, BookRepository bookRepository) {
        this.ratingRepository = ratingRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public Long create(String userId, CreateRatingRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        if (ratingRepository.existsByBookBookIdAndUserId(book.getBookId(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "한 책에는 리뷰를 하나만 작성할 수 있습니다.");
        }

        Rating rating = ratingRepository.save(new Rating(book, userId, request.rating(), request.content().trim()));
        return rating.getRatingId();
    }

    @Transactional
    public void update(String userId, UpdateRatingRequest request) {
        Rating rating = getOwnedRating(userId, request.reviewId());
        rating.update(request.rating(), request.content().trim());
    }

    @Transactional
    public void delete(String userId, Long reviewId) {
        ratingRepository.delete(getOwnedRating(userId, reviewId));
    }

    public RatingListResponse getReviews(Long bookId, int page) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다.");
        }

        Page<Rating> ratings = ratingRepository.findByBookBookId(bookId, PageRequest.of(page - 1, PAGE_SIZE, Sort.by("ratingId").descending()));
        return new RatingListResponse(
                ratings.getTotalElements(),
                ratings.map(RatingResponse::from).getContent()
        );
    }

    public RatingSummaryResponse getSummary(Long bookId) {
        RatingSummaryProjection summary = ratingRepository.getSummaryByBookId(bookId);
        double averageRating = summary == null || summary.getAverageRating() == null
                ? 0.0
                : Math.round(summary.getAverageRating() * 10.0) / 10.0;
        long reviewCount = summary == null ? 0 : summary.getReviewCount();
        return new RatingSummaryResponse(averageRating, reviewCount);
    }

    private Rating getOwnedRating(String userId, Long reviewId) {
        Rating rating = ratingRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        if (!rating.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 리뷰만 수정하거나 삭제할 수 있습니다.");
        }
        return rating;
    }
}
