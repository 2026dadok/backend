package dongsim.dadok.quest.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// GET /quests/daily 응답: { quests: [{ id, title, xp, done }], allClearXp, allClearDone }
public record DailyQuestListResponse(
        List<Item> quests,                                   // 전체 완료 보상을 뺀 일일 퀘스트
        @JsonProperty("allClearXp") int allClearXp,          // 전체 완료 보상 XP
        @JsonProperty("allClearDone") boolean allClearDone   // 전체 완료 보상 받았는지
) {
    public record Item(Long id, String title, int xp, boolean done) {}
}
