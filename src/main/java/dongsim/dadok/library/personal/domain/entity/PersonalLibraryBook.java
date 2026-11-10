package dongsim.dadok.library.personal.domain.entity;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.book.domain.entity.Book;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 사용자 한 명의 개인 서재에 추가된 책 한 권이다. */
@Entity
@Table(name = "personal_library_books", uniqueConstraints = @UniqueConstraint(
        name = "uk_personal_library_user_book", columnNames = {"user_id", "book_id"}
))
public class PersonalLibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_library_book_id")
    private Long personalLibraryBookId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "current_page", nullable = false)
    private int currentPage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "personalLibraryBook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonalReadingLog> readingLogs = new ArrayList<>();

    protected PersonalLibraryBook() {
    }

    public PersonalLibraryBook(User user, Book book) {
        this.user = user;
        this.book = book;
        this.currentPage = 0;
        this.createdAt = LocalDateTime.now();
    }

    /** 진행도가 실제로 늘어난 경우에만 true를 반환한다. */
    public boolean updateProgress(int newCurrentPage, int totalPages) {
        if (newCurrentPage == currentPage) {
            return false;
        }
        this.currentPage = newCurrentPage;
        if (newCurrentPage == totalPages && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
        return true;
    }

    public Long getPersonalLibraryBookId() { return personalLibraryBookId; }
    public User getUser() { return user; }
    public Book getBook() { return book; }
    public int getCurrentPage() { return currentPage; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
