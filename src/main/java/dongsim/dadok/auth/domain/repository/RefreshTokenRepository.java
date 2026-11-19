package dongsim.dadok.auth.domain.repository;

import dongsim.dadok.auth.domain.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(String userId);
    void deleteByUserId(String userId);

    // 재발급용 잠금 조회: 같은 사용자의 재발급을 한 줄로 세운다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.userId = :userId")
    Optional<RefreshToken> findByUserIdForUpdate(@Param("userId") String userId);
}
