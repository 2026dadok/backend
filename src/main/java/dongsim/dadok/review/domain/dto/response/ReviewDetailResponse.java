package dongsim.dadok.review.domain.dto.response;

import dongsim.dadok.review.domain.entity.Review;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
public class ReviewDetailResponse {

    private final Long reviewId;
    private final Long libBookId;
    private final String dayDisplay;
    private final String review;
    private final String reviewDate;
    private final LocalDateTime createDate;

    public ReviewDetailResponse(Review entity, int dayIndex, boolean isCompleted) {
        this.reviewId = entity.getId();
        this.libBookId = entity.getLibBookId();
        this.review = entity.getReview();
        this.createDate = entity.getCreateDate();
        this.reviewDate = entity.getReviewDate().format(DateTimeFormatter.ofPattern("yyyy. M. d"));

        this.dayDisplay = isCompleted ? dayIndex + "일차 | 완독" : dayIndex + "일차";
    }
}