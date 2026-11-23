package dongsim.dadok.auth.domain.repository;

import dongsim.dadok.auth.domain.entity.EmailVerification;
import dongsim.dadok.auth.domain.entity.Verification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email AND v.type = :type AND v.isVerified = true ORDER BY v.expiresAt DESC")
    List<EmailVerification> findVerifiedLatestList(@Param("email") String email, @Param("type") Verification type);

    default Optional<EmailVerification> findVerifiedLatest(String email, Verification type) {
        return findVerifiedLatestList(email, type).stream().findFirst();
    }

    void deleteByEmailAndType(String email, Verification type);
    Optional<EmailVerification> findTopByEmailAndTypeOrderByExpiresAtDesc(String email, Verification type);
    Optional<EmailVerification> findByEmailAndCodeAndType(String email, String code, Verification type);

    // 인증번호 비교용 잠금 조회 (동시에 대입해도 실패 횟수가 덮어써지지 않게)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailVerification> findFirstByEmailAndTypeOrderByExpiresAtDesc(String email, Verification type);
}
