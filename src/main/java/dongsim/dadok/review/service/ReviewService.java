package dongsim.dadok.review.service;

import dongsim.dadok.review.domain.dto.request.ReviewCreateRequest;
import dongsim.dadok.review.domain.dto.request.ReviewUpdateRequest;
import dongsim.dadok.review.domain.dto.response.ReviewCardResponse;
import dongsim.dadok.review.domain.dto.response.ReviewDetailResponse;
import dongsim.dadok.review.domain.entity.Review;
import dongsim.dadok.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public Long create(ReviewCreateRequest request) {
        Review review = Review.builder()
                .libBookID(request.getLibBookId())
                .reviewDate(request.getReviewDate())
                .review(request.getReview())
                .build();

        return reviewRepository.save(review).getId();
    }

    public List<ReviewCardResponse> getReviewsByLibBook(Long libBookId) {
        List<Review> reviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(libBookId);
        List<ReviewCardResponse> responses = new ArrayList<>();

        for (int i = 0; i < reviews.size(); i++) {
            int dayIndex = i + 1;
            boolean isLast = (i == reviews.size() - 1);
            responses.add(new ReviewCardResponse(reviews.get(i), dayIndex, isLast));
        }
        return responses;
    }

    public ReviewDetailResponse getReviewDetail(Long reviewId) {
        Review targetReview = findReview(reviewId);

        List<Review> allReviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(targetReview.getLibBookID());
        int dayIndex = 1;
        boolean isCompleted = false;

        for (int i = 0; i < allReviews.size(); i++) {
            if (allReviews.get(i).getId().equals(reviewId)) {
                dayIndex = i + 1;
                isCompleted = (i == allReviews.size() - 1);
                break;
            }
        }

        return new ReviewDetailResponse(targetReview, dayIndex, isCompleted);
    }

    @Transactional
    public void update(Long reviewId, ReviewUpdateRequest request) {
        Review review = findReview(reviewId);
        review.updateReview(request.getReview());   // 더티체킹으로 자동 UPDATE
    }

    @Transactional
    public void delete(Long reviewId) {
        Review review = findReview(reviewId);
        reviewRepository.delete(review);
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 감상문입니다."));
    }
}