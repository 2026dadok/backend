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
    // 감상문 등록 (모달에서 완료 버튼 클릭 시)
    @Transactional
    public Long create(ReviewCreateRequest request) {
        Review review = Review.builder()
                .libBookId(request.getLibBookId())
                .reviewDate(request.getReviewDate())
                .review(request.getReview())
                .build();

        return reviewRepository.save(review).getId();
    }

    // 특정 서재 책의 감상문 카드 전체 목록 조회 (1일차부터 순서대로 번호 매김)
    public List<ReviewCardResponse> getReviewsByLibBook(Long libBookId) {
        List<Review> reviews = reviewRepository.findAllBylibBookIdOrderByReviewDateAsc(libBookId);
    public List<ReviewCardResponse> getReviewsByLibBook(Long libBookId) {
        List<Review> reviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(libBookId);
        List<ReviewCardResponse> responses = new ArrayList<>();

        for (int i = 0; i < reviews.size(); i++) {
            int dayIndex = i + 1;
            // 마지막 작성 글 여부 (디자인의 마지막 완독 표시용 플래그)
            boolean isLast = (i == reviews.size() - 1);
            responses.add(new ReviewCardResponse(reviews.get(i), dayIndex, isLast));
        }
        return responses;
    }

    public ReviewDetailResponse getReviewDetail(Long reviewId) {
        Review targetReview = findReview(reviewId);

        List<Review> allReviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(targetReview.getLibBookID());
    // 카드 클릭 시 모달 단건 상세 조회
    public ReviewDetailResponse getReviewDetail(Long reviewId) {
        Review targetReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 감상문입니다."));

        // 등록된 전체 글 순서를 기준으로 몇 일차인지 계산
        List<Review> allReviews = reviewRepository.findAllBylibBookIdOrderByReviewDateAsc(targetReview.getLibBookId());
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
    // 감상문 내용 수정
    @Transactional
    public void update(Long reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 감상문입니다."));

        // 엔티티의 비즈니스 메서드를 호출해 변경 감지(Dirty Checking)로 자동 갱신
        review.updateReview(request.getReview());
    }

    // 감상문 삭제
    @Transactional
    public void delete(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 감상문입니다."));

        reviewRepository.delete(review);
    }
}