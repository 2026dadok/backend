package dongsim.dadok.quest.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 주간 퀘스트 (진행도 포함: 180 / 300)
public record WeeklyQuestResponse(
        @JsonProperty("quest_id") Long questId,
        String title,
        int progress,
        int target,
        @JsonProperty("reward_xp") int rewardXp,
        boolean completed
) {}
