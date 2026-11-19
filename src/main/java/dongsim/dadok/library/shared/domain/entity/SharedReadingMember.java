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
@Table(name = "shared_reading_member", uniqueConstraints = @UniqueConstraint(
        name = "uk_shared_reading_member", columnNames = {"shared_reading_id", "user_id"}
))
public class SharedReadingMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shared_reading_member_id")
    private Long sharedReadingMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_reading_id", nullable = false)
    private SharedReading sharedReading;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "current_page", nullable = false)
    private int currentPage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    protected SharedReadingMember() {
    }

    SharedReadingMember(SharedReading sharedReading, String userId) {
        this.sharedReading = sharedReading;
        this.userId = userId;
        this.currentPage = 0;
        this.joinedAt = LocalDateTime.now();
    }

    public boolean updateProgress(int currentPage, int totalPages) {
        this.currentPage = currentPage;
        // 전체 쪽수를 모르는 책(0)은 완독으로 보지 않는다
        if (totalPages > 0 && currentPage == totalPages && completedAt == null) {
            completedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public Long getSharedReadingMemberId() { return sharedReadingMemberId; }
    public String getUserId() { return userId; }
    public int getCurrentPage() { return currentPage; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
