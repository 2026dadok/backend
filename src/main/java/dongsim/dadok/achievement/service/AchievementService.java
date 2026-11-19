package dongsim.dadok.achievement.service;

import java.util.regex.Pattern;
import java.util.HashSet;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import dongsim.dadok.library.shared.repository.SharedReadingRepository;
import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;
import dongsim.dadok.library.shared.domain.entity.SharedReading;
import dongsim.dadok.library.personal.repository.PersonalLibraryBookRepository;
import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.shared.repository.DailyReflectionRepository;
import dongsim.dadok.review.repository.ReviewRepository;
import dongsim.dadok.achievement.domain.dto.AchievementResponse;
import dongsim.dadok.achievement.domain.dto.MessageResponse;
import dongsim.dadok.achievement.domain.entity.Achievement;
import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.domain.entity.UserAchievement;
import dongsim.dadok.achievement.domain.repository.AchievementRepository;
import dongsim.dadok.achievement.domain.repository.UserAchievementRepository;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.Tier;
import dongsim.dadok.quest.domain.entity.UserStat;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import lombok.RequiredArgsConstructor;
import dongsim.dadok.time.domain.event.ReadingCompletedEvent;
import java.time.LocalTime;
import java.time.LocalDate;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
@Transactional
public class AchievementService {

    private static final Logger LOG = LoggerFactory.getLogger(AchievementService.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserStatRepository userStatRepository;
    private final ReviewRepository reviewRepository;
    private final DailyReflectionRepository dailyReflectionRepository;
    private final PersonalLibraryBookRepository personalLibraryBookRepository;
    private final SharedReadingRepository sharedReadingRepository;
    private final ReadingLogRepository readingLogRepository;

    // 숫자 조건 업적 검사: value가 목표값 이상인 업적을 모두 달성 처리
    // 예) check(user, FINISH_COUNT, 10) → 완독의 맛(1), 똑똑이(10) 달성
    // 반환값: 새로 달성한 칭호명 목록
    // 독서 기록이 커밋된 뒤 별도 트랜잭션에서 처리 (여기서 실패해도 독서 기록은 남는다)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReadingCompleted(ReadingCompletedEvent event) {
        try {
            checkReadingAchievements(event.getUser(), event.isFirstRead(), event.isReadBefore(),
                    event.getStartedAt(), event.getEndedAt(), event.getToday());
            syncProgressAchievements(event.getUser());
        } catch (RuntimeException e) {
            LOG.warn("독서 업적 처리 실패 userId={}", event.getUser().getUserId(), e);
        }
    }

    private void checkReadingAchievements(User user, boolean firstRead, boolean readBefore,
                                          LocalDateTime startedAt, LocalDateTime endedAt, LocalDate today) {
        if (firstRead) {
            check(user, AchievementCondition.FIRST_READ, 1);
        }
        if (readBefore) {
            check(user, AchievementCondition.RESUME_READ, 1);
        }
        if (isNightTime(startedAt.toLocalTime()) || isNightTime(endedAt.toLocalTime())) {
            check(user, AchievementCondition.NIGHT_READ, 1);
        }
    }

    private boolean isNightTime(LocalTime time) {
        return time.getHour() >= 23 || time.getHour() < 5;
    }

    public List<String> check(User user, AchievementCondition condition, int value) {
        List<String> newly = new ArrayList<>();
        for (Achievement a : achievementRepository.findAllByConditionType(condition)) {
            int target = a.getTargetValue() == null ? 1 : a.getTargetValue();
            if (value >= target && grant(user, a)) {
                newly.add(a.getTitle());
            }
        }
        return newly;
    }

    // 완독·특정 도서·공동서재·연속 독서·단번에 완독·다시 읽기 업적을 DB 기록으로 다시 판정한다 (이미 받은 업적은 건너뜀)
    // 완독할 때, 독서 기록 저장 뒤, 메인 페이지·마이페이지 조회 때 부른다. 자기 업적만 다룬다 (다른 사람 행을 잠그지 않음)
    public List<String> syncProgressAchievements(User user) {
        List<String> newly = new ArrayList<>();
        String userId = user.getUserId();
        List<PersonalLibraryBook> finishedPersonal = personalLibraryBookRepository.findAllByUserAndCompletedAtIsNotNull(user);
        List<SharedReading> shared = sharedReadingRepository.findDistinctByMembersUserIdOrderByCreatedAtDesc(userId);
        List<SharedReading> finishedShared = shared.stream()
                .filter(s -> s.getMembers().stream().anyMatch(m -> userId.equals(m.getUserId()) && m.getCompletedAt() != null))
                .toList();

        // 완독 권수 (개인 서재와 공동서재에서 같은 책은 한 권으로 센다)
        Set<Long> finishedBookIds = new HashSet<>();
        List<String> finishedTitles = new ArrayList<>();
        finishedPersonal.forEach(b -> {
            finishedBookIds.add(b.getBook().getBookId());
            finishedTitles.add(b.getBook().getBookTitle());
        });
        finishedShared.forEach(s -> {
            finishedBookIds.add(s.getBook().getBookId());
            finishedTitles.add(s.getBook().getBookTitle());
        });
        newly.addAll(check(user, AchievementCondition.FINISH_COUNT, finishedBookIds.size()));

        // 특정 도서 완독
        for (Achievement a : achievementRepository.findAllByConditionType(AchievementCondition.FINISH_BOOK)) {
            if (a.getTargetBook() != null && finishedTitles.stream().anyMatch(t -> isSameBook(t, a.getTargetBook()))
                    && grant(user, a)) {
                newly.add(a.getTitle());
            }
        }

        // 공동서재: 함께 완독(둘 다 완독해서 종료), 먼저 완독
        newly.addAll(check(user, AchievementCondition.JOINT_FINISH,
                (int) shared.stream().filter(s -> s.getStatus() == SharedReadingStatus.COMPLETED).count()));
        newly.addAll(check(user, AchievementCondition.JOINT_WIN,
                (int) shared.stream().filter(s -> userId.equals(s.getWinnerUserId())).count()));

        // 연속 독서일 (가장 길었던 기록)
        newly.addAll(check(user, AchievementCondition.STREAK, maxStreak(readingLogRepository.findReadDatesByUser(user))));

        // 개인 서재 완독 책: 타이머 한 번에 완독, 완독 뒤 다시 읽기
        for (PersonalLibraryBook b : finishedPersonal) {
            Long bookId = b.getBook().getBookId();
            if (readingLogRepository.countByUserAndBookId(user, bookId) == 1) {
                newly.addAll(check(user, AchievementCondition.ONE_SESSION_FINISH, 1));
            }
            // 완독한 그 독서(같은 시각에 저장)와 구분하려고 1분 뒤부터 본다
            if (readingLogRepository.existsByUserAndBookIdAndEndedAtAfter(user, bookId, b.getCompletedAt().plusMinutes(1))) {
                newly.addAll(check(user, AchievementCondition.REREAD, 1));
            }
        }
        return newly;
    }

    // 책 제목이 업적의 책과 같은지: 제목이 같거나, 업적 책 이름 뒤에 부제·권 표시가 붙은 경우 (예: "데미안 (민음사)", "신곡: 지옥편")
    private static boolean isSameBook(String title, String target) {
        if (title == null) return false;
        String t = title.trim();
        return t.equals(target) || Pattern.compile("^" + Pattern.quote(target) + "[\\s(:\\[\\-,·.]").matcher(t).find();
    }

    // 날짜 목록(오름차순, 중복 없음)에서 가장 길게 이어진 일수
    private static int maxStreak(List<LocalDate> dates) {
        int max = 0;
        int current = 0;
        LocalDate prev = null;
        for (LocalDate date : dates) {
            current = (prev != null && prev.plusDays(1).equals(date)) ? current + 1 : 1;
            max = Math.max(max, current);
            prev = date;
        }
        return max;
    }

    // 감상문 작성 개수 업적: 개인 서재 감상문 + 공동서재 감상문
    public List<String> checkFeelingCount(String userId) {
        User user = findUser(userId);
        long count = reviewRepository.countByWriter(user) + dailyReflectionRepository.countByUserId(userId);
        return check(user, AchievementCondition.FEELING_COUNT, (int) count);
    }

    // 순위 조건 업적 검사: rank가 목표 순위 이내인 업적을 달성 처리
    // 예) checkRank(user, WEEKLY_RANK_ENTER, 1) → 독서왕(10위), 일일천하(1위) 달성
    public List<String> checkRank(User user, AchievementCondition condition, int rank) {
        List<String> newly = new ArrayList<>();
        for (Achievement a : achievementRepository.findAllByConditionType(condition)) {
            if (a.getTargetValue() != null && rank <= a.getTargetValue() && grant(user, a)) {
                newly.add(a.getTitle());
            }
        }
        return newly;
    }

    // 티어 업적 검사: 현재 티어까지의 업적을 모두 달성 처리
    public List<String> checkTier(User user, int xp) {
        return check(user, AchievementCondition.TIER_REACH, Tier.of(xp).level());
    }

    // 업적 목록 + 달성 여부 + 사용 여부
    public List<AchievementResponse> getAchievements(String userId) {
        User user = findUser(userId);
        UserStat stat = getOrCreateStat(user);
        checkTier(user, stat.getXp());
        syncProgressAchievements(user);

        Set<Long> achievedIds = userAchievementRepository.findAchievementIdsByUser(user);
        Long equippedId = stat.getEquippedAchievementId();

        return achievementRepository.findAllByOrderByIdAsc().stream()
                .map(a -> new AchievementResponse(
                        a.getId(),
                        a.getTitle(),
                        a.getDescription(),
                        achievedIds.contains(a.getId()),
                        a.getId().equals(equippedId)))
                .toList();
    }

    // 칭호 사용 (PUT /users/title)
    public MessageResponse equipTitle(String userId, Long achievementId) {
        User user = findUser(userId);
        UserStat stat = getOrCreateStat(user);
        checkTier(user, stat.getXp());

        if (!achievementRepository.existsById(achievementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 칭호입니다.");
        }
        if (!userAchievementRepository.existsByUserAndAchievement_Id(user, achievementId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "달성하지 않은 칭호입니다.");
        }

        stat.equipTitle(achievementId);
        return new MessageResponse("칭호가 변경되었습니다.");
    }

    // 사용 중인 칭호명 (없으면 null)
    @Transactional(readOnly = true)
    public String getEquippedTitle(User user) {
        return userStatRepository.findByUser(user)
                .map(UserStat::getEquippedAchievementId)
                .flatMap(achievementRepository::findById)
                .map(Achievement::getTitle)
                .orElse(null);
    }

    private boolean grant(User user, Achievement achievement) {
        if (userAchievementRepository.existsByUserAndAchievement(user, achievement)) {
            return false;
        }
        // 같은 사용자의 동시 요청이 같은 업적을 두 번 저장하지 않게 잠근 뒤 다시 확인한다
        userStatRepository.lockOrCreate(user);
        if (userAchievementRepository.existsByUserAndAchievement(user, achievement)) {
            return false;
        }
        userAchievementRepository.save(new UserAchievement(user, achievement, LocalDateTime.now(KST)));
        return true;
    }

    private UserStat getOrCreateStat(User user) {
        return userStatRepository.findByUser(user)
                .orElseGet(() -> userStatRepository.lockOrCreate(user));
    }

    private User findUser(String userId) {
        return userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
