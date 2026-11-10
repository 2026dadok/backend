package dongsim.dadok.achievement.domain.repository;

import dongsim.dadok.achievement.domain.entity.Achievement;
import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findAllByConditionType(AchievementCondition conditionType);

    List<Achievement> findAllByOrderByIdAsc();
}
