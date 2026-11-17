package dongsim.dadok.review.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.personal.repository.PersonalLibraryBookRepository;
import dongsim.dadok.review.domain.dto.request.ReviewCreateRequest;
import dongsim.dadok.review.domain.dto.request.ReviewUpdateRequest;
import dongsim.dadok.review.domain.dto.response.ReviewCardResponse;
import dongsim.dadok.review.domain.dto.response.ReviewDetailResponse;
import dongsim.dadok.review.domain.entity.Review;
import dongsim.dadok.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PersonalLibraryBookRepository personalLibraryBookRepository;

    // 감상문 등록 (모달에서 완료 버튼 클릭 시) - 본인 서재 책에만 쓸 수 있다
    @Transactional
    public Long create(String userId, ReviewCreateRequest request) {
        requireOwnedLibBook(userId, request.getLibBookId());

        Review review = Review.builder()
                .libBookId(request.getLibBookId())
                .reviewDate(request.getReviewDate())
                .review(request.getReview())
                .build();

        return reviewRepository.save(review).getId();
    }

    // 특정 서재 책의 감상문 카드 전체 목록 조회 (1일차부터 순서대로 번호 매김)
    public List<ReviewCardResponse> getReviewsByLibBook(String userId, Long libBookId) {
        // 감상문은 서재 책에 달리므로, 서재 책의 주인이 곧 작성자다
        User writer = requireOwnedLibBook(userId, libBookId).getUser();

        List<Review> reviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(libBookId);
        List<ReviewCardResponse> responses = new ArrayList<>();

        for (int i = 0; i < reviews.size(); i++) {
            int dayIndex = i + 1;
            // 마지막 작성 글 여부 (디자인의 마지막 완독 표시용 플래그)
            boolean isLast = (i == reviews.size() - 1);
            responses.add(new ReviewCardResponse(reviews.get(i), dayIndex, isLast,
                    writer.getUserId(), writer.getName()));
        }
        return responses;
    }

    // 카드 클릭 시 모달 단건 상세 조회
    public ReviewDetailResponse getReviewDetail(String userId, Long reviewId) {
        Review targetReview = findOwnedReview(userId, reviewId);
        User writer = requireOwnedLibBook(userId, targetReview.getLibBookId()).getUser();

        // 등록된 전체 글 순서를 기준으로 몇 일차인지 계산
        List<Review> allReviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(targetReview.getLibBookId());
        int dayIndex = 1;
        boolean isCompleted = false;

        for (int i = 0; i < allReviews.size(); i++) {
            if (allReviews.get(i).getId().equals(reviewId)) {
                dayIndex = i + 1;
                isCompleted = (i == allReviews.size() - 1);
                break;
            }
        }
        return new ReviewDetailResponse(targetReview, dayIndex, isCompleted,
                writer.getUserId(), writer.getName());
    }

    // 감상문 내용 수정 (변경 감지로 자동 갱신)
    @Transactional
    public void update(String userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = findOwnedReview(userId, reviewId);
        review.updateReview(request.getReview());
    }

    // 감상문 삭제
    @Transactional
    public void delete(String userId, Long reviewId) {
        Review review = findOwnedReview(userId, reviewId);
        reviewRepository.delete(review);
    }

    // 감상문을 찾고, 그 감상문이 달린 서재 책이 로그인한 사용자의 것인지 확인한다.
    private Review findOwnedReview(String userId, Long reviewId) {
        requireLogin(userId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 감상문입니다."));
        requireOwnedLibBook(userId, review.getLibBookId());
        return review;
    }

    // 서재 책이 존재하고 로그인한 사용자의 것인지 확인한다.
    // 없는 책이면 404, 남의 책이면 403 을 돌려준다.
    private PersonalLibraryBook requireOwnedLibBook(String userId, Long libBookId) {
        requireLogin(userId);
        PersonalLibraryBook libBook = personalLibraryBookRepository.findById(libBookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "개인 서재에 추가되지 않은 책입니다."));
        if (!userId.equals(libBook.getUser().getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 서재의 감상문만 이용할 수 있습니다.");
        }
        return libBook;
    }

    private void requireLogin(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}
