package dongsim.dadok.rating.domain.dto;

import dongsim.dadok.rating.domain.entity.Rating;

public record RatingSummaryResponse(double averageRating, long reviewCount) {
}
