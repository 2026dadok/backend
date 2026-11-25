package dongsim.dadok.quest.service;

import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.dto.AttendanceResponse;
import dongsim.dadok.quest.domain.dto.DailyQuestListResponse;
import dongsim.dadok.quest.domain.dto.DailyQuestResponse;
import dongsim.dadok.quest.domain.dto.WeeklyQuestResponse;
import dongsim.dadok.quest.domain.dto.WeeklyQuestSummaryResponse;
import dongsim.dadok.quest.domain.entity.*;
import dongsim.dadok.quest.domain.repository.QuestRepository;
import dongsim.dadok.quest.domain.repository.UserQuestRepository;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.event.ReadingCompletedEvent;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestService {

    private static final Logger LOG = LoggerFactory.getLogger(QuestService.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserStatRepository userStatRepository;
    private final ReadingLogRepository readingLogRepository;
    // 독서 기록이 커밋된 뒤 별도 트랜잭션에서 처리 (여기서 실패해도 독서 기록은 남는다)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReadingCompleted(ReadingCompletedEvent event) {
        try {
            // 동시에 저장된 기록까지 포함하도록 DB에서 다시 합산한다
            syncReadTimeQuests(event.getUser(), event.getToday());
        } catch (RuntimeException e) {
            LOG.warn("독서 시간 퀘스트 처리 실패 userId={}", event.getUser().getUserId(), e);
        }
    }


    private final AchievementService achievementService;

    // 출석 (POST /quests/attendance)
    public AttendanceResponse attend(String userId) {
        User user = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        Quest attendance = questRepository.findFirstByConditionType(QuestCondition.ATTENDANCE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "출석 퀘스트가 없습니다."));

        LocalDate today = LocalDate.now(KST);

        // 하루 1번만
        if (isCompleted(user, attendance, today)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘은 이미 출석했습니다.");
        }

        int gainedXp = complete(user, attendance, today);
        return new AttendanceResponse("출석 완료!", gainedXp);
    }

    // 독서 시간 퀘스트 (30분 / 2시간)
    // 독서 시간이 저장될 때마다 ReadingService가 부름
    // todaySeconds: 오늘 읽은 시간 합계 (초)
    public int checkReadTimeQuests(User user, long todaySeconds, LocalDate today) {
        int gainedXp = 0;
        for (Quest quest : questRepository.findAllByConditionType(QuestCondition.READ_MINUTES)) {
            long targetSeconds = quest.getTargetValue() * 60L;   // 30분 → 1800초
            if (todaySeconds >= targetSeconds && !isCompleted(user, quest, today)) {
                gainedXp += complete(user, quest, today);
            }
        }
        return gainedXp;
    }

    // 새 책을 개인 서재에 등록했을 때 (일일 "새로운 도서 등록하기")
    public int onBookRegistered(User user) {
        LocalDate today = LocalDate.now(KST);
        int gainedXp = 0;
        for (Quest quest : questRepository.findAllByConditionType(QuestCondition.BOOK_REGISTER)) {
            if (!isCompleted(user, quest, today)) {
                gainedXp += complete(user, quest, today);
            }
        }
        return gainedXp;
    }

    // 읽은 쪽수가 늘었을 때 (주간 "이번 주 N페이지 읽기")
    public int addReadPages(User user, int pages) {
        if (pages <= 0) return 0;
        // 같은 사용자의 요청이 동시에 와도 진행도가 덮어써지지 않게 사용자별로 잠근다
        lockUser(user);
        LocalDate monday = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int gainedXp = 0;
        for (Quest quest : questRepository.findAllByConditionType(QuestCondition.READ_PAGES)) {
            UserQuest record = userQuestRepository.findByUserAndQuestAndPeriodStart(user, quest, monday)
                    .orElseGet(() -> userQuestRepository.save(UserQuest.inProgress(user, quest, monday)));
            if (record.getCompletedAt() != null) continue;
            record.addProgress(pages);
            if (record.getProgress() >= quest.getTargetValue()) {
                record.markCompleted();
                gainedXp += giveXp(user, quest.getRewardXp());
            }
        }
        return gainedXp;
    }

    // 퀘스트 완료 공통 처리
    // 출석뿐 아니라 독서 시간, 책 등록 등 다른 기능에서도 이걸 부름
    // 반환값: 이번에 받은 XP 합계
    public int complete(User user, Quest quest, LocalDate periodStart) {
        lockUser(user);
        Optional<UserQuest> existing = userQuestRepository.findByUserAndQuestAndPeriodStart(
                user, quest, periodStart
        );
        if (existing.isPresent()) {
            UserQuest userQuest = existing.get();
            if (userQuest.getCompletedAt() != null) {
                return 0; // 동시 요청이나 이전 요청으로 이미 완료된 경우
            }
            userQuest.addProgress(quest.getTargetValue());
            userQuest.markCompleted();
        } else {
            userQuestRepository.save(UserQuest.completed(user, quest, periodStart));
        }
        int gainedXp = giveXp(user, quest.getRewardXp());

        // 일일 퀘스트를 하나 끝낼 때마다 "전체 완료" 검사
        if (quest.getPeriod() == QuestPeriod.DAILY && quest.getConditionType() != QuestCondition.ALL_DAILY) {
            gainedXp += checkAllDailyClear(user, periodStart);
        }
        return gainedXp;
    }

    // 일일 퀘스트 4개를 다 끝냈으면 전체 완료 보상(30XP) 자동 지급
    private int checkAllDailyClear(User user, LocalDate today) {
        Quest allClear = null;
        boolean allDone = true;

        for (Quest q : questRepository.findAllByPeriod(QuestPeriod.DAILY)) {
            if (q.getConditionType() == QuestCondition.ALL_DAILY) {
                allClear = q;
            } else if (!isCompleted(user, q, today)) {
                allDone = false;
            }
        }

        if (allClear == null || !allDone || isCompleted(user, allClear, today)) {
            return 0;
        }
        userQuestRepository.save(UserQuest.completed(user, allClear, today));
        return giveXp(user, allClear.getRewardXp());
    }

    // 오늘의 일일 퀘스트 목록 + 완료 여부 (마지막 항목: 전체 완료 보상)
    @Transactional(readOnly = true)
    // 일일 퀘스트 목록 (전체 완료 보상은 제외 - getAllClearQuest로 따로 조회)
    public List<DailyQuestResponse> getDailyQuests(User user) {
        LocalDate today = LocalDate.now(KST);
        return questRepository.findAllByPeriod(QuestPeriod.DAILY).stream()
                .filter(q -> q.getConditionType() != QuestCondition.ALL_DAILY)
                .sorted(Comparator.comparing(Quest::getId))
                .map(q -> new DailyQuestResponse(q.getId(), q.getTitle(), q.getRewardXp(), isCompleted(user, q, today)))
                .toList();
    }

    // 일일 퀘스트 전체 완료 보상 (없으면 null)
    public DailyQuestResponse getAllClearQuest(User user) {
        LocalDate today = LocalDate.now(KST);
        return questRepository.findAllByPeriod(QuestPeriod.DAILY).stream()
                .filter(q -> q.getConditionType() == QuestCondition.ALL_DAILY)
                .findFirst()
                .map(q -> new DailyQuestResponse(q.getId(), q.getTitle(), q.getRewardXp(), isCompleted(user, q, today)))
                .orElse(null);
    }

    // 이번 주 주간 퀘스트 + 진행도 (없으면 null)
    @Transactional(readOnly = true)
    public WeeklyQuestResponse getWeeklyQuest(User user) {
        LocalDate monday = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return questRepository.findAllByPeriod(QuestPeriod.WEEKLY).stream()
                .min(Comparator.comparing(Quest::getId))
                .map(q -> {
                    Optional<UserQuest> record = userQuestRepository.findByUserAndQuestAndPeriodStart(user, q, monday);
                    int progress = record.map(UserQuest::getProgress).orElse(0);
                    boolean completed = record.map(r -> r.getCompletedAt() != null).orElse(false);
                    return new WeeklyQuestResponse(q.getId(), q.getTitle(), progress, q.getTargetValue(), q.getRewardXp(), completed);
                })
                .orElse(null);
    }

    // 독서 기록은 저장됐는데 퀘스트 처리가 빠진 경우(처리 중 서버 종료, 처리 실패)를 메운다.
    // 오늘 읽은 시간 합계로 다시 판정하고, 이미 완료한 퀘스트는 건너뛴다
    private void syncReadTimeQuests(User user, LocalDate today) {
        checkReadTimeQuests(user, readingLogRepository.sumSecondsByUserAndDate(user, today), today);
    }

    // 메인 페이지 조회 때 추가 보정: 독서 기록이 있는데 "첫 독서" 업적이 빠졌으면 채운다 (이미 있으면 건너뜀)
    private void syncFirstRead(User user) {
        if (readingLogRepository.existsByUser(user)) {
            achievementService.check(user, AchievementCondition.FIRST_READ, 1);
        }
    }

    // 메인 페이지 컨트롤러가 조회 전에 부른다 (자기 트랜잭션에서 처리)
    public void syncReadTimeQuests(String userId) {
        User user = findUser(userId);
        syncReadTimeQuests(user, LocalDate.now(KST));
        syncFirstRead(user);
        achievementService.syncProgressAchievements(user);
    }

    // GET /quests/daily
    public DailyQuestListResponse getDailyQuestList(String userId) {
        User user = findUser(userId);
        LocalDate today = LocalDate.now(KST);
        syncReadTimeQuests(user, today);
        List<DailyQuestListResponse.Item> items = new java.util.ArrayList<>();
        int allClearXp = 0;
        boolean allClearDone = false;
        for (Quest q : questRepository.findAllByPeriod(QuestPeriod.DAILY).stream()
                .sorted(Comparator.comparing(Quest::getId)).toList()) {
            boolean done = isCompleted(user, q, today);
            if (q.getConditionType() == QuestCondition.ALL_DAILY) {
                allClearXp = q.getRewardXp();
                allClearDone = done;
            } else {
                items.add(new DailyQuestListResponse.Item(q.getId(), q.getTitle(), q.getRewardXp(), done));
            }
        }
        return new DailyQuestListResponse(items, allClearXp, allClearDone);
    }

    // GET /quests/weekly
    @Transactional(readOnly = true)
    public WeeklyQuestSummaryResponse getWeeklyQuestSummary(String userId) {
        WeeklyQuestResponse w = getWeeklyQuest(findUser(userId));
        if (w == null) {
            return new WeeklyQuestSummaryResponse("", 0, 0, 0, false);
        }
        return new WeeklyQuestSummaryResponse(w.title(), w.progress(), w.target(), w.rewardXp(), w.completed());
    }

    private User findUser(String userId) {
        return userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    // 누적 XP (기록이 없으면 0)
    @Transactional(readOnly = true)
    public int getXp(User user) {
        return userStatRepository.findByUser(user).map(UserStat::getXp).orElse(0);
    }

    // 퀘스트 외 보상 XP 지급 (주간 정산, 공동 서재 승리 등)
    public int rewardXp(User user, int amount) {
        return amount > 0 ? giveXp(user, amount) : 0;
    }

    private boolean isCompleted(User user, Quest quest, LocalDate periodStart) {
        return userQuestRepository.existsByUserAndQuestAndPeriodStartAndCompletedAtIsNotNull(user, quest, periodStart);
    }

    // 사용자별 잠금: 같은 사용자의 XP·퀘스트 변경을 한 줄로 세운다 (트랜잭션이 끝날 때까지 유지)
    // 읽기 격리수준은 READ COMMITTED (application.yaml) 라서 잠금 뒤에는 다른 요청이 커밋한 값이 보인다
    public UserStat lockUser(User user) {
        return userStatRepository.lockOrCreate(user);
    }

    // XP 지급: 기록(UserStat)이 없으면 새로 만들고 더함
    private int giveXp(User user, int amount) {
        UserStat stat = lockUser(user);
        stat.addXp(amount);
        achievementService.checkTier(user, stat.getXp());
        return amount;
    }
}
