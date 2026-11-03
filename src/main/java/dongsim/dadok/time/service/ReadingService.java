package dongsim.dadok.time.service;

import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.league.service.LeagueService;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.quest.service.QuestService;
import dongsim.dadok.time.domain.dto.ReadTimeRequest;
import dongsim.dadok.time.domain.dto.ReadTimeResponse;
import dongsim.dadok.time.domain.dto.TimerPageResponse;
import dongsim.dadok.time.domain.entity.ReadingLog;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long MAX_SECONDS_PER_DAY = 24 * 60 * 60;

    private final ReadingLogRepository readingLogRepository;
    private final UserStatRepository userStatRepository;
    private final QuestService questService;
    private final AchievementService achievementService;
    private final LeagueService leagueService;

    // 독서 시간 저장 (POST /times)
    public ReadTimeResponse saveReadTime(String email, ReadTimeRequest request) {
        User user = findUser(email);

        LocalDateTime now = LocalDateTime.now(KST);
        LocalDate today = now.toLocalDate();

        // 하루 합계 24시간 넘으면 거부 (시간 조작 방지)
        long before = readingLogRepository.sumSecondsByUserAndDate(user, today);
        if (before + request.readTime() > MAX_SECONDS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "하루 독서 시간은 24시간을 넘을 수 없습니다.");
        }

        // 저장 전 상태 (업적 판정용)
        boolean firstRead = !readingLogRepository.existsByUser(user);
        boolean readBefore = readingLogRepository.existsByUserAndBookId(user, request.bookId());

        // 1. 기록 저장
        readingLogRepository.save(new ReadingLog(user, request.bookId(), today, request.readTime(), now));

        // 2. 오늘 합계로 30분 / 2시간 퀘스트 검사
        long todaySeconds = before + request.readTime();
        questService.checkReadTimeQuests(user, todaySeconds, today);

        // 3. 독서 관련 업적 검사
        checkReadingAchievements(user, firstRead, readBefore, now.minusSeconds(request.readTime()), now, today);

        // 4. 주간 경쟁 참가 + 순위 업적 검사
        leagueService.onReadingSaved(user);

        return new ReadTimeResponse("독서 시간이 저장되었습니다.", todaySeconds);
    }

    // 타이머 페이지 (GET /times/timer_page)
    @Transactional(readOnly = true)
    public TimerPageResponse getTimerPage(String email) {
        User user = findUser(email);
        long goalSeconds = user.getDefaultTimerTime() * 60L;   // 분 → 초
        long todaySeconds = readingLogRepository.sumSecondsByUserAndDate(user, LocalDate.now(KST));
        return new TimerPageResponse(goalSeconds, todaySeconds);
    }

    private void checkReadingAchievements(User user, boolean firstRead, boolean readBefore,
                                          LocalDateTime startedAt, LocalDateTime endedAt, LocalDate today) {
        if (firstRead) {
            achievementService.check(user, AchievementCondition.FIRST_READ, 1);
        }
        // TODO: 완독 여부 연결 후 미완독 책만 책갈피, 완독한 책은 다시 읽는 자로 구분
        if (readBefore) {
            achievementService.check(user, AchievementCondition.RESUME_READ, 1);
        }
        if (isNightTime(startedAt.toLocalTime()) || isNightTime(endedAt.toLocalTime())) {
            achievementService.check(user, AchievementCondition.NIGHT_READ, 1);
        }
        achievementService.check(user, AchievementCondition.STREAK,
                currentStreak(readingLogRepository.findReadDatesByUser(user), today));
    }

    // 23:00 ~ 05:00
    private boolean isNightTime(LocalTime time) {
        return time.getHour() >= 23 || time.getHour() < 5;
    }

    // 오늘까지 이어진 연속 독서일
    private int currentStreak(List<LocalDate> dates, LocalDate today) {
        int streak = 0;
        LocalDate expected = today;
        for (int i = dates.size() - 1; i >= 0; i--) {
            if (!dates.get(i).equals(expected)) break;
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }

    private User findUser(String email) {
        return userStatRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
