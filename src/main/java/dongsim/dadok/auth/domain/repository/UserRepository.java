package dongsim.dadok.auth.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByNameAndBirthDateAndEmail(String name, LocalDate birthDate, String email);
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);

    // 사용자 행 잠금 (같은 사용자의 동시 요청을 한 줄로 세움)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);
}
