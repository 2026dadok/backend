package dongsim.dadok.note.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.book.domain.entity.Book;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 인용구 노트 (책에서 마음에 든 문장)
@Entity
@Table(name = "quote_notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "book_page", nullable = false)
    private int bookPage;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(name = "content", nullable = false, length = 200)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Note() {
    }

    public Note(User user, Book book, int bookPage, LocalDate noteDate, String content) {
        this.user = user;
        this.book = book;
        this.bookPage = bookPage;
        this.noteDate = noteDate;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public void update(int bookPage, LocalDate noteDate, String content) {
        this.bookPage = bookPage;
        this.noteDate = noteDate;
        this.content = content;
    }

    public Long getNoteId() { return noteId; }
    public User getUser() { return user; }
    public Book getBook() { return book; }
    public int getBookPage() { return bookPage; }
    public LocalDate getNoteDate() { return noteDate; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
