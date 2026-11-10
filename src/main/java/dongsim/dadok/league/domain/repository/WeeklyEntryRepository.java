package dongsim.dadok.league.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.league.domain.entity.WeeklyEntry;
import dongsim.dadok.quest.domain.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyEntryRepository extends JpaRepository<WeeklyEntry, Long> {

    Optional<WeeklyEntry> findByUserAndWeekStart(User user, LocalDate weekStart);

    // 같은 주, 같은 리그 참가자
    List<WeeklyEntry> findAllByWeekStartAndTier(LocalDate weekStart, Tier tier);

    // 아직 정산 안 된 주의 참가 기록 (이번 주 제외)
    List<WeeklyEntry> findAllBySettledFalseAndWeekStartBefore(LocalDate weekStart);

    // 아직 안 본 정산 결과 (가장 최근 주)
    Optional<WeeklyEntry> findFirstByUserAndSettledTrueAndResultSeenFalseOrderByWeekStartDesc(User user);

    // N위 이내로 마무리한 횟수
    long countByUserAndFinalRankLessThanEqual(User user, int rank);
}
