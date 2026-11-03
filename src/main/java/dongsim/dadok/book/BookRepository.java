package dongsim.dadok.book;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByBookTitleContaining(String title);
    boolean existsByIsbn13(String isbn13);
}