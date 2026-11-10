package dongsim.dadok.quest.init;

import dongsim.dadok.quest.domain.entity.Quest;
import dongsim.dadok.quest.domain.entity.QuestCondition;
import dongsim.dadok.quest.domain.entity.QuestPeriod;
import dongsim.dadok.quest.domain.repository.QuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 서버가 켜질 때 퀘스트 목록이 비어 있으면 6개를 자동으로 넣음
@Component
@RequiredArgsConstructor
public class QuestInitializer implements ApplicationRunner {

    private final QuestRepository questRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (questRepository.count() > 0) return;

        questRepository.saveAll(List.of(
                quest("일일 출석하기",         QuestPeriod.DAILY,  QuestCondition.ATTENDANCE,    1,   10),
                quest("30분 독서하기",          QuestPeriod.DAILY,  QuestCondition.READ_MINUTES,  30,  5),
                quest("2시간 이상 독서하기",     QuestPeriod.DAILY,  QuestCondition.READ_MINUTES,  120, 10),
                quest("새로운 도서 등록하기",    QuestPeriod.DAILY,  QuestCondition.BOOK_REGISTER, 1,   10),
                quest("일일 퀘스트 전체 완료",   QuestPeriod.DAILY,  QuestCondition.ALL_DAILY,     4,   30),
                quest("이번주에 300페이지 읽기", QuestPeriod.WEEKLY, QuestCondition.READ_PAGES,    300, 20)
        ));
    }

    private Quest quest(String title, QuestPeriod period, QuestCondition condition, int target, int xp) {
        return Quest.builder()
                .title(title).period(period).conditionType(condition)
                .targetValue(target).rewardXp(xp)
                .build();
    }
}
