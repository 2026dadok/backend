package dongsim.dadok.library.shared.repository;

import dongsim.dadok.library.shared.domain.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyReflectionRepository extends JpaRepository<DailyReflection, Long> {
    boolean existsBySharedReadingSharedReadingIdAndUserIdAndReadingDay(Long sharedReadingId, String userId, int readingDay);

    List<DailyReflection> findBySharedReadingSharedReadingIdOrderByReadingDayDescCreatedAtDesc(Long sharedReadingId);

    List<DailyReflection> findBySharedReadingSharedReadingIdAndUserIdOrderByReadingDayDescCreatedAtDesc(Long sharedReadingId, String userId);

    long countByUserId(String userId);
}
