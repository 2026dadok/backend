package dongsim.dadok.rating.domain.dto;

import dongsim.dadok.rating.domain.entity.Rating;

import java.util.List;

public record RatingListResponse(long total, List<RatingResponse> reviews) {
}
