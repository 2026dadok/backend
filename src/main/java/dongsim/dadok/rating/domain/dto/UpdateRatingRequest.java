package dongsim.dadok.rating.domain.dto;

import dongsim.dadok.rating.domain.entity.Rating;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRatingRequest(
        @JsonProperty("reviewID") @NotNull Long reviewId,
        @NotBlank @Size(max = 255) String content,
        @NotNull @Min(1) @Max(5) Integer rating
) {
}
