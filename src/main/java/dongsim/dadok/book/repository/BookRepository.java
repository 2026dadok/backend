package dongsim.dadok.book.repository;

import java.util.List;
import dongsim.dadok.book.domain.entity.Book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByBookTitleContaining(String title, Pageable pageable);
    Optional<Book> findByIsbn13(String isbn13);

    List<Book> findAllByIsbn13In(java.util.Collection<String> isbn13s);
    Optional<Book> findFirstByBookTitleAndAuthor(String bookTitle, String author);
}
