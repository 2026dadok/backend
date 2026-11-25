package dongsim.dadok.review.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiReviewSaveResponse(
        @JsonProperty("review_id") Long reviewId,
        @JsonProperty("review_content") String reviewContent
) {
}
