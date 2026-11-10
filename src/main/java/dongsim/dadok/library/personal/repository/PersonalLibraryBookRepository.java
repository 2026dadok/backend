package dongsim.dadok.library.personal.repository;

import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.personal.domain.entity.PersonalReadingLog;

import dongsim.dadok.auth.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalLibraryBookRepository extends JpaRepository<PersonalLibraryBook, Long> {

    boolean existsByUserAndBookBookId(User user, Long bookId);

    @EntityGraph(attributePaths = "book")
    Optional<PersonalLibraryBook> findByUserAndBookBookId(User user, Long bookId);

    @EntityGraph(attributePaths = "book")
    List<PersonalLibraryBook> findByUserOrderByCreatedAtDesc(User user);
}
