package dongsim.dadok.quest.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.Quest;
import dongsim.dadok.quest.domain.entity.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    // 이 유저가 이 퀘스트를 이 기간에 이미 완료했나?
    boolean existsByUserAndQuestAndPeriodStartAndCompletedAtIsNotNull(
            User user, Quest quest, LocalDate periodStart);

    // 이 기간의 진행 기록 (없으면 아직 시작 안 함)
    Optional<UserQuest> findByUserAndQuestAndPeriodStart(User user, Quest quest, LocalDate periodStart);
}
