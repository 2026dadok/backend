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
import dongsim.dadok.time.domain.entity.ReadingLog;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import dongsim.dadok.library.personal.repository.PersonalLibraryBookRepository;
import dongsim.dadok.time.domain.event.ReadingCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
@Transactional
public class LeagueService {

    private static final Logger LOG = LoggerFactory.getLogger(LeagueService.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WeeklyEntryRepository weeklyEntryRepository;
    private final ReadingLogRepository readingLogRepository;
    private final UserStatRepository userStatRepository;
    private final AchievementService achievementService;
    private final ProfileImageService profileImageService;
    private final BookRepository bookRepository;
    private final PersonalLibraryBookRepository personalLibraryBookRepository;

    private static final int COMPETITOR_BOOKS_MAX = 5;

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
                competitorBooks(myEntry.getTier(), monday, me),
                myRank,
                ranking,
                readingBook(me),
                lastWeek
        );
    }

    // 이번 주 참가 기록 (없으면 현재 티어로 리그 배정)
    public WeeklyEntry getOrCreateEntry(User user, LocalDate monday) {
        return weeklyEntryRepository.findByUserAndWeekStart(user, monday)
                .orElseGet(() -> weeklyEntryRepository.save(new WeeklyEntry(user, monday, Tier.of(getXp(user)))));
    }

    // 독서 시간 저장 이벤트 → 리그 참가 처리
    // 독서 기록이 커밋된 뒤 별도 트랜잭션에서 처리 (여기서 실패해도 독서 기록은 남는다)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReadingCompleted(ReadingCompletedEvent event) {
        try {
            onReadingSaved(event.getUser());
        } catch (RuntimeException e) {
            LOG.warn("주간 리그 참가 처리 실패 userId={}", event.getUser().getUserId(), e);
        }
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

    // 같은 리그 경쟁자들이 이번 주에 읽은 책 (최근 순, 중복 제외, 최대 5권)
    private List<Object> competitorBooks(Tier tier, LocalDate monday, User me) {
        List<User> others = weeklyEntryRepository.findAllByWeekStartAndTier(monday, tier).stream()
                .map(WeeklyEntry::getUser)
                .filter(u -> !u.getId().equals(me.getId()))
                .toList();
        if (others.isEmpty()) return List.of();

        LinkedHashSet<Long> bookIds = new LinkedHashSet<>();
        for (ReadingLog log : readingLogRepository.findByUserInAndReadDateGreaterThanEqualOrderByEndedAtDesc(others, monday)) {
            if (log.getBookId() != null) bookIds.add(log.getBookId());
            if (bookIds.size() >= COMPETITOR_BOOKS_MAX) break;
        }
        List<Object> result = new ArrayList<>();
        for (Book book : bookRepository.findAllById(bookIds)) {
            result.add(bookInfo(book));
        }
        // findAllById 는 순서를 보장하지 않으므로 최근 순으로 다시 정렬
        List<Long> order = new ArrayList<>(bookIds);
        result.sort(Comparator.comparingInt(b -> order.indexOf((Long) ((Map<?, ?>) b).get("book_id"))));
        return result;
    }

    // 내가 가장 최근에 읽은 책 + 개인 서재 진행률 (없으면 null)
    private Object readingBook(User me) {
        return readingLogRepository.findFirstByUserOrderByEndedAtDesc(me)
                .map(ReadingLog::getBookId)
                .flatMap(bookRepository::findById)
                .map(book -> {
                    Map<String, Object> info = bookInfo(book);
                    personalLibraryBookRepository.findByUserAndBookBookId(me, book.getBookId()).ifPresent(lib -> {
                        int total = book.getPages();
                        info.put("read_page", lib.getCurrentPage());
                        info.put("total_page", total);
                        info.put("percent", total <= 0 ? 0 : (int) Math.min(100L, lib.getCurrentPage() * 100L / total));
                    });
                    return (Object) info;
                })
                .orElse(null);
    }

    private Map<String, Object> bookInfo(Book book) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("book_id", book.getBookId());
        info.put("title", book.getBookTitle());
        info.put("author", book.getAuthor());
        info.put("cover_url", book.getCoverUrl());
        info.put("isbn", book.getIsbn13());
        info.put("total_page", book.getPages());
        return info;
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
