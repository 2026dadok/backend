package dongsim.dadok.quest.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.UserStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserStatRepository extends JpaRepository<UserStat, Long> {

    Optional<UserStat> findByUser(User user);

    // 토큰의 아이디로 User 조회
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findUserByUserId(String userId);
}
