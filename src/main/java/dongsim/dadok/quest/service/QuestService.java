package dongsim.dadok.quest.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.dto.AttendanceResponse;
import dongsim.dadok.quest.domain.entity.*;
import dongsim.dadok.quest.domain.repository.QuestRepository;
import dongsim.dadok.quest.domain.repository.UserQuestRepository;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserStatRepository userStatRepository;

    // ───── 출석 (POST /quests/attendance) ─────
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

    // ───── 퀘스트 완료 공통 처리 ─────
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

    private boolean isCompleted(User user, Quest quest, LocalDate periodStart) {
        return userQuestRepository.existsByUserAndQuestAndPeriodStartAndCompletedAtIsNotNull(user, quest, periodStart);
    }

    // XP 지급: 기록(UserStat)이 없으면 새로 만들고 더함
    private int giveXp(User user, int amount) {
        UserStat stat = userStatRepository.findByUser(user)
                .orElseGet(() -> userStatRepository.save(new UserStat(user)));
        stat.addXp(amount);
        return amount;
    }
}
