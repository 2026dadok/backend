package dongsim.dadok.library.shared.domain.entity;

import dongsim.dadok.book.domain.entity.Book;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shared_reading")
public class SharedReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shared_reading_id")
    private Long sharedReadingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "creator_user_id", nullable = false, length = 100)
    private String creatorUserId;

    @Column(name = "pair_key", nullable = false, length = 220)
    private String pairKey;

    @Column(name = "winner_user_id", length = 100)
    private String winnerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharedReadingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "sharedReading", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedReadingMember> members = new ArrayList<>();

    protected SharedReading() {
    }

    public SharedReading(Book book, String creatorUserId, String pairKey) {
        this.book = book;
        this.creatorUserId = creatorUserId;
        this.pairKey = pairKey;
        this.status = SharedReadingStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void addMember(String userId) {
        members.add(new SharedReadingMember(this, userId));
    }

    public void decideWinner(String userId) {
        if (winnerUserId == null) {
            winnerUserId = userId;
            status = SharedReadingStatus.WINNER_DECIDED;
        }
    }

    public void complete() {
        status = SharedReadingStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public Long getSharedReadingId() { return sharedReadingId; }
    public Book getBook() { return book; }
    public String getCreatorUserId() { return creatorUserId; }
    public String getPairKey() { return pairKey; }
    public String getWinnerUserId() { return winnerUserId; }
    public SharedReadingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public List<SharedReadingMember> getMembers() { return members; }
}
