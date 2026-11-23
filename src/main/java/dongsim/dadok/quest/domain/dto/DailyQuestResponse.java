package dongsim.dadok.quest.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 일일 퀘스트 1개
public record DailyQuestResponse(
        @JsonProperty("quest_id") Long questId,
        String title,
        @JsonProperty("reward_xp") int rewardXp,
        boolean completed
) {}
