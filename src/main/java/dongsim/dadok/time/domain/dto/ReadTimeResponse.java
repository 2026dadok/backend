package dongsim.dadok.time.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 응답: 200, message, today_readtime
public record ReadTimeResponse(
        String message,
        @JsonProperty("today_readtime") long todayReadTime   // 오늘 읽은 시간 합계 (초)
) {}
