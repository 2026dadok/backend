package dongsim.dadok.ai.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiInterviewResponse(
        @JsonProperty("next_question") String nextQuestion,
        @JsonProperty("is_ready_to_review") boolean readyToReview
) {
}
