package dongsim.dadok.league.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 지난주 결과 알림: "주간 경쟁을 12등으로 끝났습니다 + 90 XP 획득"
public record LastWeekResultResponse(
        Integer rank,
        @JsonProperty("gained_xp") int gainedXp
) {}
