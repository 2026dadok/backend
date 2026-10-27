package dongsim.dadok.auth.domain.repository;

import dongsim.dadok.auth.domain.entity.EmailVerification;
import dongsim.dadok.auth.domain.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndTypeOrderByExpiresAtDesc(String email, Verification type);
    Optional<EmailVerification> findByEmailAndCodeAndType(String email, String code, Verification type);
}
