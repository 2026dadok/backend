package dongsim.dadok.time.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 응답: 200, goal_readtime, today_readtime (단위: 초)
public record TimerPageResponse(
        @JsonProperty("goal_readtime") long goalReadTime,     // 하루 목표 독서 시간
        @JsonProperty("today_readtime") long todayReadTime    // 오늘 읽은 시간 합계
) {}
