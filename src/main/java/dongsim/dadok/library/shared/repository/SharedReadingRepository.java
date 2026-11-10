package dongsim.dadok.library.shared.repository;

import dongsim.dadok.library.shared.domain.entity.*;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedReadingRepository extends JpaRepository<SharedReading, Long> {
    boolean existsByBookBookIdAndPairKeyAndStatusIn(Long bookId, String pairKey, List<SharedReadingStatus> statuses);

    @EntityGraph(attributePaths = {"book", "members"})
    Optional<SharedReading> findWithBookAndMembersBySharedReadingId(Long sharedReadingId);

    @EntityGraph(attributePaths = {"book", "members"})
    List<SharedReading> findDistinctByMembersUserIdOrderByCreatedAtDesc(String userId);
}
