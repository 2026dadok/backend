package dongsim.dadok.quest.service;

import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.dto.AttendanceResponse;
import dongsim.dadok.quest.domain.dto.DailyQuestResponse;
import dongsim.dadok.quest.domain.dto.WeeklyQuestResponse;
import dongsim.dadok.quest.domain.entity.*;
import dongsim.dadok.quest.domain.repository.QuestRepository;
import dongsim.dadok.quest.domain.repository.UserQuestRepository;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.event.ReadingCompletedEvent;
import org.springframework.context.event.EventListener;
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

@Service
@RequiredArgsConstructor
@Transactional
public class QuestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserStatRepository userStatRepository;
    @EventListener
    public void handleReadingCompleted(ReadingCompletedEvent event) {
        checkReadTimeQuests(event.getUser(), event.getTodaySeconds(), event.getToday());
    }


    private final AchievementService achievementService;

    // 출석 (POST /quests/attendance)
    public AttendanceResponse attend(String email) {
        User user = userStatRepository.findUserByEmail(email)
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

    // 퀘스트 완료 공통 처리
    // 출석뿐 아니라 독서 시간, 책 등록 등 다른 기능에서도 이걸 부름
    // 반환값: 이번에 받은 XP 합계
    public int complete(User user, Quest quest, LocalDate periodStart) {
        userQuestRepository.save(UserQuest.completed(user, quest, periodStart));
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
    public List<DailyQuestResponse> getDailyQuests(User user) {
        LocalDate today = LocalDate.now(KST);
        return questRepository.findAllByPeriod(QuestPeriod.DAILY).stream()
                .sorted(Comparator.comparing(Quest::getId))
                .map(q -> new DailyQuestResponse(q.getId(), q.getTitle(), q.getRewardXp(), isCompleted(user, q, today)))
                .toList();
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

    // XP 지급: 기록(UserStat)이 없으면 새로 만들고 더함
    private int giveXp(User user, int amount) {
        UserStat stat = userStatRepository.findByUser(user)
                .orElseGet(() -> userStatRepository.save(new UserStat(user)));
        stat.addXp(amount);
        achievementService.checkTier(user, stat.getXp());
        return amount;
    }
}
