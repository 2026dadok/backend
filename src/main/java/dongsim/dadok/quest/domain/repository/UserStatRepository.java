package dongsim.dadok.quest.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.UserStat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserStatRepository extends JpaRepository<UserStat, Long> {

    Optional<UserStat> findByUser(User user);

    // 사용자별 잠금 (같은 사용자의 퀘스트 진행도 동시 갱신을 한 줄로 세움)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserStat s where s.user = :user")
    Optional<UserStat> findByUserForUpdate(User user);

    // 토큰의 아이디로 User 조회
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findUserByUserId(String userId);
}
