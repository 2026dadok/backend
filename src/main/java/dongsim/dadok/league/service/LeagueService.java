package dongsim.dadok.league.service;

import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.league.domain.dto.LastWeekResultResponse;
import dongsim.dadok.league.domain.dto.RankResponse;
import dongsim.dadok.league.domain.dto.WeekCompetitionResponse;
import dongsim.dadok.league.domain.entity.WeeklyEntry;
import dongsim.dadok.league.domain.repository.WeeklyEntryRepository;
import dongsim.dadok.profile.service.ProfileImageService;
import dongsim.dadok.quest.domain.entity.Tier;
import dongsim.dadok.quest.domain.entity.UserStat;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import dongsim.dadok.time.domain.event.ReadingCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LeagueService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WeeklyEntryRepository weeklyEntryRepository;
    private final ReadingLogRepository readingLogRepository;
    private final UserStatRepository userStatRepository;
    private final AchievementService achievementService;
    private final ProfileImageService profileImageService;

    // 주간 경쟁 페이지 (GET /main/week_competition)
    public WeekCompetitionResponse getWeekCompetition(String userId) {
        User me = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        LocalDate monday = thisMonday();
        WeeklyEntry myEntry = getOrCreateEntry(me, monday);
        List<RankResponse> ranking = buildRanking(myEntry.getTier(), monday, me);

        RankResponse myRank = ranking.stream().filter(RankResponse::isMe).findFirst().orElse(null);

        // 아직 안 본 지난주 결과가 있으면 한 번만 보여줌
        LastWeekResultResponse lastWeek = weeklyEntryRepository
                .findFirstByUserAndSettledTrueAndResultSeenFalseOrderByWeekStartDesc(me)
                .map(entry -> {
                    entry.markResultSeen();
                    return new LastWeekResultResponse(entry.getFinalRank(), entry.getRewardXp());
                })
                .orElse(null);

        return new WeekCompetitionResponse(
                myEntry.getTier().getDisplayName(),
                List.of(),      // TODO: 경쟁자들이 읽는 책 (책 정보 연결)
                myRank,
                ranking,
                null,           // TODO: 내가 읽고 있는 책 + 진행률 (책 정보 연결)
                lastWeek
        );
    }

    // 이번 주 참가 기록 (없으면 현재 티어로 리그 배정)
    public WeeklyEntry getOrCreateEntry(User user, LocalDate monday) {
        return weeklyEntryRepository.findByUserAndWeekStart(user, monday)
                .orElseGet(() -> weeklyEntryRepository.save(new WeeklyEntry(user, monday, Tier.of(getXp(user)))));
    }

    // 독서 시간 저장 이벤트 → 리그 참가 처리
    @EventListener
    public void handleReadingCompleted(ReadingCompletedEvent event) {
        onReadingSaved(event.getUser());
    }

    // 독서 시간이 저장된 뒤 호출: 이번 주 리그 참가 + 현재 순위로 업적 검사
    public void onReadingSaved(User user) {
        LocalDate monday = thisMonday();
        WeeklyEntry entry = getOrCreateEntry(user, monday);
        buildRanking(entry.getTier(), monday, user).stream()
                .filter(RankResponse::isMe)
                .map(RankResponse::rank)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(rank -> achievementService.checkRank(user, AchievementCondition.WEEKLY_RANK_ENTER, rank));
    }

    // 주간 경쟁 5위권 진입 횟수 (마이페이지)
    @Transactional(readOnly = true)
    public int countTop5(User user) {
        return (int) weeklyEntryRepository.countByUserAndFinalRankLessThanEqual(user, 5);
    }

    // 리그 순위: 독서 시간 많은 순, 0분은 순위 없음(null)
    private List<RankResponse> buildRanking(Tier tier, LocalDate monday, User me) {
        List<WeeklyEntry> entries = weeklyEntryRepository.findAllByWeekStartAndTier(monday, tier);
        Map<Long, Long> seconds = sumSeconds(entries, monday);

        List<WeeklyEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong((WeeklyEntry e) -> seconds.getOrDefault(e.getUser().getId(), 0L)).reversed());

        List<RankResponse> result = new ArrayList<>();
        int rank = 0;
        for (WeeklyEntry e : sorted) {
            User u = e.getUser();
            long total = seconds.getOrDefault(u.getId(), 0L);
            Integer displayRank = total > 0 ? ++rank : null;
            result.add(new RankResponse(
                    displayRank,
                    u.getName(),
                    achievementService.getEquippedTitle(u),
                    profileImageService.profileUrl(u),
                    total,
                    u.getId().equals(me.getId())));
        }
        return result;
    }

    // 리그 참가자별 그 주 독서 시간 합계
    public Map<Long, Long> sumSeconds(List<WeeklyEntry> entries, LocalDate monday) {
        Map<Long, Long> result = new HashMap<>();
        if (entries.isEmpty()) return result;
        List<User> users = entries.stream().map(WeeklyEntry::getUser).toList();
        for (Object[] row : readingLogRepository.sumSecondsByUsersBetween(users, monday, monday.plusDays(6))) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private int getXp(User user) {
        return userStatRepository.findByUser(user).map(UserStat::getXp).orElse(0);
    }

    private LocalDate thisMonday() {
        return LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
