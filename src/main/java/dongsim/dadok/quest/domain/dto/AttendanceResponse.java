package dongsim.dadok.quest.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 출석 응답: 200, message, reward_xp
public record AttendanceResponse(
        String message,
        @JsonProperty("reward_xp") int rewardXp   // 이번에 받은 XP (전체 완료 보상 포함)
) {}
