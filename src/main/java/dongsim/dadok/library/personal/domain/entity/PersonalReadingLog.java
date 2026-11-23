package dongsim.dadok.library.personal.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 개인 서재에서 실제로 진행도를 높인 시점을 기록해 독서 캘린더에 사용한다. */
@Entity
@Table(name = "personal_reading_logs")
public class PersonalReadingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_reading_log_id")
    private Long personalReadingLogId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personal_library_book_id", nullable = false)
    private PersonalLibraryBook personalLibraryBook;

    @Column(name = "read_date", nullable = false)
    private LocalDate readDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PersonalReadingLog() {
    }

    public PersonalReadingLog(PersonalLibraryBook personalLibraryBook, LocalDate readDate) {
        this.personalLibraryBook = personalLibraryBook;
        this.readDate = readDate;
        this.createdAt = LocalDateTime.now();
    }

    public LocalDate getReadDate() { return readDate; }
}
