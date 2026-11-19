package dongsim.dadok.library.shared.repository;

import dongsim.dadok.library.shared.domain.entity.*;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SharedReadingRepository extends JpaRepository<SharedReading, Long> {
    boolean existsByBookBookIdAndPairKeyAndStatusIn(Long bookId, String pairKey, List<SharedReadingStatus> statuses);

    @EntityGraph(attributePaths = {"book", "members"})
    Optional<SharedReading> findWithBookAndMembersBySharedReadingId(Long sharedReadingId);

    @EntityGraph(attributePaths = {"book", "members"})
    List<SharedReading> findDistinctByMembersUserIdOrderByCreatedAtDesc(String userId);

    // 쪽수 저장 때 공동서재 행을 잠근다 (두 사람이 동시에 완독해도 완료 처리가 빠지지 않게)
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SharedReading s where s.sharedReadingId = :id")
    Optional<SharedReading> lockById(@Param("id") Long id);

    // 사용자가 공동서재에서 읽은 쪽수 합계 (마이페이지 총 독서 쪽수)
    @Query("select coalesce(sum(m.currentPage), 0) from SharedReadingMember m where m.userId = :userId")
    long sumCurrentPageByUserId(@Param("userId") String userId);
}
