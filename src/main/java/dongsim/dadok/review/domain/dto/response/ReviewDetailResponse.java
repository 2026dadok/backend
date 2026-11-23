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
    private final String writerId;   // 작성자 로그인 아이디
    private final String writerName; // 작성자 이름

    public ReviewDetailResponse(Review entity, int dayIndex, boolean isCompleted, String writerId, String writerName) {
        this.reviewId = entity.getId();
        this.libBookId = entity.getLibBookId();
        this.review = entity.getReview();
        this.createDate = entity.getCreateDate();
        this.reviewDate = entity.getReviewDate().format(DateTimeFormatter.ofPattern("yyyy. M. d"));

        this.dayDisplay = isCompleted ? dayIndex + "일차 | 완독" : dayIndex + "일차";
        this.writerId = writerId;
        this.writerName = writerName;
    }
}