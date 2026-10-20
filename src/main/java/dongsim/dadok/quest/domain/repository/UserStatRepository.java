package dongsim.dadok.quest.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.UserStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserStatRepository extends JpaRepository<UserStat, Long> {

    Optional<UserStat> findByUser(User user);

    // 로그인 토큰에는 이메일이 들어 있어서, 이메일로 User를 찾음
    // (친구의 auth 패키지를 건드리지 않으려고 여기서 직접 조회)
    @Query("select u from User u where u.email = :email")
    Optional<User> findUserByEmail(String email);
}
