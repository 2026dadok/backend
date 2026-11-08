package dongsim.dadok.rating.controller;

import dongsim.dadok.rating.domain.dto.*;
import dongsim.dadok.rating.service.RatingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@Validated
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<RatingListResponse> getReviews(
            @PathVariable Long bookId,
            @RequestParam(value = "page", defaultValue = "1") @Positive int page
    ) {
        return ResponseEntity.ok(ratingService.getReviews(bookId, page));
    }

    @PostMapping("/books/reviews")
    public ResponseEntity<Map<String, Long>> createReview(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateRatingRequest request
    ) {
        Long reviewId = ratingService.create(requireUser(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("review_id", reviewId));
    }

    @PutMapping("/books/reviews")
    public ResponseEntity<Map<String, String>> updateReview(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateRatingRequest request
    ) {
        ratingService.update(requireUser(userId), request);
        return ResponseEntity.ok(Map.of("message", "리뷰가 수정되었습니다."));
    }

    @DeleteMapping("/books/reviews")
    public ResponseEntity<Map<String, String>> deleteReview(
            @AuthenticationPrincipal String userId,
            @RequestParam("reviewID") Long reviewId
    ) {
        ratingService.delete(requireUser(userId), reviewId);
        return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }
}
