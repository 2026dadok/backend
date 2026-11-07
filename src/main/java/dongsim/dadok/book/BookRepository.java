package dongsim.dadok.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByBookTitleContaining(String title, Pageable pageable);
    Optional<Book> findByIsbn13(String isbn13);
}
