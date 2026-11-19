package dongsim.dadok.library.personal.repository;

import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.personal.domain.entity.PersonalReadingLog;

import dongsim.dadok.auth.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PersonalLibraryBookRepository extends JpaRepository<PersonalLibraryBook, Long> {

    boolean existsByUserAndBookBookId(User user, Long bookId);

    @EntityGraph(attributePaths = "book")
    Optional<PersonalLibraryBook> findByUserAndBookBookId(User user, Long bookId);

    @EntityGraph(attributePaths = "book")
    List<PersonalLibraryBook> findByUserOrderByCreatedAtDesc(User user);

    // 완독한 책 (업적 판정)
    List<PersonalLibraryBook> findAllByUserAndCompletedAtIsNotNull(User user);

    // 개인 서재에서 읽은 쪽수 합계 (마이페이지)
    @Query("select coalesce(sum(p.currentPage), 0) from PersonalLibraryBook p where p.user = :user")
    long sumCurrentPageByUser(User user);
}
