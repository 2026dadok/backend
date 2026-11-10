package dongsim.dadok.auth.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserId(String userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByNameAndBirthDateAndEmail(String name, LocalDate birthDate, String email);
    boolean existsByUserId(String userId);
    boolean existsByEmail(String email);
}
