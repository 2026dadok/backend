package dongsim.dadok.league.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 응답: 200, tier, competitors_books, my_rank, ranking, reading_book, last_week_result
public record WeekCompetitionResponse(
        String tier,
        @JsonProperty("competitors_books") List<Object> competitorsBooks,
        @JsonProperty("my_rank") RankResponse myRank,
        List<RankResponse> ranking,
        @JsonProperty("reading_book") Object readingBook,
        @JsonProperty("last_week_result") LastWeekResultResponse lastWeekResult
) {}
