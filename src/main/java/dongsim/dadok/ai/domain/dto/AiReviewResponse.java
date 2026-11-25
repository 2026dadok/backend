package dongsim.dadok.ai.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiReviewResponse(
        @JsonProperty("review_content") String reviewContent
) {
}
