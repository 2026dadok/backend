package dongsim.dadok.rating.domain.projection;

public interface RatingSummaryProjection {
    Double getAverageRating();

    long getReviewCount();
}
