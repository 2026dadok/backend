package dongsim.dadok.quest.domain.repository;

import dongsim.dadok.quest.domain.entity.Quest;
import dongsim.dadok.quest.domain.entity.QuestCondition;
import dongsim.dadok.quest.domain.entity.QuestPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    // 조건 종류로 퀘스트 1개 찾기 (예: ATTENDANCE → 일일 출석하기)
    Optional<Quest> findFirstByConditionType(QuestCondition conditionType);

    // 조건 종류로 퀘스트 여러 개 찾기 (예: READ_MINUTES → 30분, 2시간)
    List<Quest> findAllByConditionType(QuestCondition conditionType);

    // 주기별 퀘스트 목록 (예: DAILY → 일일 퀘스트 전부)
    List<Quest> findAllByPeriod(QuestPeriod period);
}
