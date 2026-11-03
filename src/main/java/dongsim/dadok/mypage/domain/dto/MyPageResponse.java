package dongsim.dadok.mypage.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 응답: 200, all_reading_time, sequence_day, all_reading_page, league_in5
public record MyPageResponse(
        @JsonProperty("all_reading_time") long allReadingTime,   // 총 독서 시간 (초)
        @JsonProperty("sequence_day") int sequenceDay,           // 최대 연속 독서일
        @JsonProperty("all_reading_page") int allReadingPage,    // 총 독서 페이지
        @JsonProperty("league_in5") int leagueIn5                // 주간 경쟁 5위권 진입 횟수
) {}
