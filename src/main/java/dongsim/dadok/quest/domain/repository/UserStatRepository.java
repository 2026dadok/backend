package dongsim.dadok.quest.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.UserStat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserStatRepository extends JpaRepository<UserStat, Long> {

    Optional<UserStat> findByUser(User user);

    // 사용자별 잠금 (같은 사용자의 퀘스트 진행도 동시 갱신을 한 줄로 세움)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserStat s where s.user = :user")
    Optional<UserStat> findByUserForUpdate(User user);

    // 사용자 행 잠금 (UserStat 이 아직 없을 때도 잠글 대상이 있게)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockUserRow(@Param("id") Long id);

    // 사용자 기록(UserStat)을 잠금 상태로 가져오고, 없으면 만든다.
    // 사용자 행을 먼저 잠가서 처음 요청 두 개가 동시에 와도 한 번만 만든다. UserStat 을 새로 만드는 곳은 모두 이것을 쓴다
    default UserStat lockOrCreate(User user) {
        lockUserRow(user.getId());
        return findByUserForUpdate(user).orElseGet(() -> saveAndFlush(new UserStat(user)));
    }

    // 아이디로 User 를 잠금 조회 (비밀번호 변경·탈퇴가 로그인과 겹치지 않게)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> lockUserByUserId(@Param("userId") String userId);

    // 토큰의 아이디로 User 조회
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findUserByUserId(String userId);
}
