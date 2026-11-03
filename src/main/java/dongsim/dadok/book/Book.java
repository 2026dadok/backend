package dongsim.dadok.book;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book_storage")
@Getter
@NoArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "publisher", nullable = false, length = 100)
    private String publisher;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Column(name = "genre", nullable = false, length = 50)
    private String genre;

    @Column(name = "isbn13", length = 20, unique = true)
    private String isbn13;

    @Column(name = "pages", nullable = false)
    private Integer pages;

    public Book(String bookTitle, String coverUrl, String publisher, String author, String genre, String isbn13, Integer pages) {
        this.bookTitle = bookTitle;
        this.coverUrl = coverUrl;
        this.publisher = publisher;
        this.author = author;
        this.genre = genre;
        this.isbn13 = isbn13;
        this.pages = pages;
    }
}
