package dongsim.dadok.league.service;

import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.league.domain.entity.WeeklyEntry;
import dongsim.dadok.league.domain.repository.WeeklyEntryRepository;
import dongsim.dadok.quest.domain.entity.Tier;
import dongsim.dadok.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

// 주간 정산: 매주 월요일 0시에 지난주 순위 확정 + 보상 XP + 업적
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklySettlementService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WeeklyEntryRepository weeklyEntryRepository;
    private final LeagueService leagueService;
    private final QuestService questService;
    private final AchievementService achievementService;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void settle() {
        LocalDate thisMonday = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 이번 주 이전의 정산 안 된 기록 전부 (서버가 꺼져 있어 놓친 주도 포함)
        Map<LocalDate, Map<Tier, List<WeeklyEntry>>> groups = weeklyEntryRepository
                .findAllBySettledFalseAndWeekStartBefore(thisMonday).stream()
                .collect(Collectors.groupingBy(WeeklyEntry::getWeekStart,
                        Collectors.groupingBy(WeeklyEntry::getTier)));

        groups.forEach((weekStart, byTier) ->
                byTier.values().forEach(entries -> settleLeague(entries, weekStart)));

        log.info("주간 정산 완료: {}주", groups.size());
    }

    private void settleLeague(List<WeeklyEntry> entries, LocalDate weekStart) {
        Map<Long, Long> seconds = leagueService.sumSeconds(entries, weekStart);

        List<WeeklyEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong((WeeklyEntry e) -> seconds.getOrDefault(e.getUser().getId(), 0L)).reversed());

        int rank = 0;
        for (WeeklyEntry e : sorted) {
            User user = e.getUser();
            long total = seconds.getOrDefault(user.getId(), 0L);
            Integer finalRank = total > 0 ? ++rank : null;
            int reward = finalRank == null ? 0 : rewardFor(finalRank);

            e.settle(finalRank, total, reward);
            questService.rewardXp(user, reward);
            if (finalRank != null) {
                achievementService.checkRank(user, AchievementCondition.WEEKLY_RANK_FINISH, finalRank);
                achievementService.checkRank(user, AchievementCondition.WEEKLY_RANK_ENTER, finalRank);
            }
        }
    }

    // 순위별 보상 XP
    private int rewardFor(int rank) {
        if (rank == 1) return 200;
        if (rank == 2) return 170;
        if (rank == 3) return 150;
        if (rank <= 5) return 130;
        if (rank <= 10) return 110;
        if (rank <= 20) return 90;
        return 50;
    }
}
