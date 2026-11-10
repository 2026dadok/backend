package dongsim.dadok.rating.domain.dto;

import dongsim.dadok.rating.domain.entity.Rating;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RatingResponse(
        @JsonProperty("reviewID") Long reviewId,
        @JsonProperty("userID") String userId,
        String content,
        int rating
) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getRatingId(),
                rating.getUserId(),
                rating.getRatingReview(),
                rating.getRatingScore()
        );
    }
}
