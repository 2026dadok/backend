package dongsim.dadok.book.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book_storage")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookId;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "publisher", length = 100)
    private String publisher;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "genre", length = 50)
    private String genre;

    @Column(name = "isbn13", unique = true, length = 50)
    private String isbn13;

    @Column(name = "pages")
    private int pages;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // YES24 월간 베스트셀러 목록에 한 번이라도 오른 책 (베스트셀러 업적). 표시 전에는 null
    @Column(name = "bestseller")
    private Boolean bestseller;

    public Book() {}

    public Book(String bookTitle, String coverUrl, String publisher, String author, String genre, String isbn13, int pages, String description) {
        this.bookTitle = bookTitle;
        this.coverUrl = coverUrl;
        this.publisher = publisher;
        this.author = author;
        this.genre = genre;
        this.isbn13 = isbn13;
        this.pages = pages;
        this.description = description;
    }

    public Long getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getCoverUrl() { return coverUrl; }
    public String getPublisher() { return publisher; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public String getIsbn13() { return isbn13; }
    public int getPages() { return pages; }
    public String getDescription() { return description; }

    public boolean isBestseller() { return Boolean.TRUE.equals(bestseller); }

    public void markBestseller() {
        this.bestseller = true;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
