package dongsim.dadok.review.controller;

import dongsim.dadok.ai.domain.dto.AiInterviewRequest;
import dongsim.dadok.ai.domain.dto.AiInterviewResponse;
import dongsim.dadok.review.domain.dto.request.AiReviewSaveRequest;
import dongsim.dadok.review.domain.dto.request.ReviewCreateRequest;
import dongsim.dadok.review.domain.dto.request.ReviewUpdateRequest;
import dongsim.dadok.review.domain.dto.response.AiReviewSaveResponse;
import dongsim.dadok.review.domain.dto.response.ReviewCardResponse;
import dongsim.dadok.review.domain.dto.response.ReviewDetailResponse;
import dongsim.dadok.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// 감상문 API: 모든 요청은 로그인이 필요하고, 본인 서재 책의 감상문만 다룰 수 있다.
// userId 는 JwtAuthenticationFilter 가 토큰에서 꺼내 넣어 준 로그인 아이디다.
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/interview")
    public ResponseEntity<AiInterviewResponse> interview(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AiInterviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.interview(userId, request));
    }

    @PostMapping("/generate")
    public ResponseEntity<AiReviewSaveResponse> generateAndSave(
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AiReviewSaveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.generateAndSave(userId, request, idempotencyKey));
    }

    @PostMapping
    public ResponseEntity<Long> createReview(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        Long reviewId = reviewService.create(userId, request);
        return ResponseEntity.ok(reviewId);
    }

    @GetMapping
    public ResponseEntity<List<ReviewCardResponse>> getReviewByLibBook(
            @AuthenticationPrincipal String userId,
            @RequestParam Long libBookId
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByLibBook(userId, libBookId));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDetailResponse> getReviewDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(reviewService.getReviewDetail(userId, reviewId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @AuthenticationPrincipal String userId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        reviewService.update(userId, reviewId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal String userId,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok().build();
    }
}
