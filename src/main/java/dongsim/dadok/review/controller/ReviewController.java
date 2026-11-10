package dongsim.dadok.review.controller;

import dongsim.dadok.review.domain.dto.request.ReviewCreateRequest;
import dongsim.dadok.review.domain.dto.request.ReviewUpdateRequest;
import dongsim.dadok.review.domain.dto.response.ReviewCardResponse;
import dongsim.dadok.review.domain.dto.response.ReviewDetailResponse;
import dongsim.dadok.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Long> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        Long reviewId = reviewService.create(request);
        return ResponseEntity.ok(reviewId);
    }

    @GetMapping
    public ResponseEntity<List<ReviewCardResponse>> getReviewByLibBook(@RequestParam Long libBookId) {
        return ResponseEntity.ok(reviewService.getReviewsByLibBook(libBookId));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDetailResponse> getReviewDetail(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReviewDetail(reviewId));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        reviewService.update(reviewId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.delete(reviewId);
        return ResponseEntity.ok().build();
    }
}