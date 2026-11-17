package dongsim.dadok.review.domain.dto.response;

import dongsim.dadok.review.domain.entity.Review;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class ReviewCardResponse {

    private final Long reviewId;
    private final Long libBookId;
    private final int dayIndex;
    private final String dayDisplay;
    private final String review;
    private final String reviewDate;
    private final boolean isCompleted;
    private final String writerId;   // 작성자 로그인 아이디
    private final String writerName; // 작성자 이름

    public ReviewCardResponse(Review entity, int index, boolean isCompleted, String writerId, String writerName) {
        this.reviewId = entity.getId();
        this.libBookId = entity.getLibBookId();
        this.dayIndex = index;
        this.isCompleted = isCompleted;
        this.review = entity.getReview();
        this.reviewDate = entity.getReviewDate().format(DateTimeFormatter.ofPattern("yyyy. M. d"));

        this.dayDisplay = isCompleted ? index + "일차 / 완독" : index + "일차";
        this.writerId = writerId;
        this.writerName = writerName;
    }
}