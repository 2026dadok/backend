package dongsim.dadok.achievement.domain.repository;

import dongsim.dadok.achievement.domain.entity.Achievement;
import dongsim.dadok.achievement.domain.entity.UserAchievement;
import dongsim.dadok.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    boolean existsByUserAndAchievement(User user, Achievement achievement);

    boolean existsByUserAndAchievement_Id(User user, Long achievementId);

    // 달성한 업적 id 목록
    @Query("select ua.achievement.id from UserAchievement ua where ua.user = :user")
    Set<Long> findAchievementIdsByUser(User user);
}
