package dongsim.dadok.achievement.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 업적 1개 (마이페이지 업적 목록)
public record AchievementResponse(
        @JsonProperty("achievement_id") Long achievementId,
        String title,
        String description,
        boolean achieved,       // 달성 여부
        boolean equipped        // 사용 중 여부
) {}
