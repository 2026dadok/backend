package dongsim.dadok.home.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dongsim.dadok.quest.domain.dto.DailyQuestResponse;
import dongsim.dadok.quest.domain.dto.WeeklyQuestResponse;

import java.util.List;

// 응답: 200, xp, my_bookstore, public_bookstore, daily_quest, weekly_quest, recommand_book, quote
public record MainPageResponse(
        int xp,
        @JsonProperty("my_bookstore") List<Object> myBookstore,
        @JsonProperty("public_bookstore") List<Object> publicBookstore,
        @JsonProperty("daily_quest") List<DailyQuestResponse> dailyQuest,
        @JsonProperty("weekly_quest") WeeklyQuestResponse weeklyQuest,
        @JsonProperty("recommand_book") List<Object> recommandBook,
        Object quote
) {}
