package dongsim.dadok.library.shared.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "daily_reflection", uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_reflection", columnNames = {"shared_reading_id", "user_id", "reading_day"}
))
public class DailyReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reflection_id")
    private Long reflectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_reading_id", nullable = false)
    private SharedReading sharedReading;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "reading_day", nullable = false)
    private int readingDay;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected DailyReflection() {
    }

    public DailyReflection(SharedReading sharedReading, String userId, int readingDay, String content) {
        this.sharedReading = sharedReading;
        this.userId = userId;
        this.readingDay = readingDay;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getReflectionId() { return reflectionId; }
    public String getUserId() { return userId; }
    public int getReadingDay() { return readingDay; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
