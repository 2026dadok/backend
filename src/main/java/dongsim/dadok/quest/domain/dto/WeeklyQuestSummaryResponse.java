package dongsim.dadok.quest.domain.dto;

// GET /quests/weekly 응답: { title, current, goal, xp, done }
public record WeeklyQuestSummaryResponse(
        String title,
        int current,   // 진행도
        int goal,      // 목표
        int xp,        // 보상 XP
        boolean done
) {}
