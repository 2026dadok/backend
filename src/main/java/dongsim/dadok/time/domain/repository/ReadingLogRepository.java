package dongsim.dadok.time.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.time.domain.entity.ReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {

    // 특정 날짜에 읽은 시간 합계 (초). 기록이 없으면 0
    @Query("select coalesce(sum(r.seconds), 0) from ReadingLog r where r.user = :user and r.readDate = :date")
    long sumSecondsByUserAndDate(User user, LocalDate date);

    // 전체 독서 시간 합계 (초)
    @Query("select coalesce(sum(r.seconds), 0) from ReadingLog r where r.user = :user")
    long sumSecondsByUser(User user);

    // 책을 읽은 날짜 목록 (중복 제거, 오래된 순)
    // 여러 사용자의 기간 내 독서 기록 (최근 순) - 주간 경쟁 "경쟁자들이 읽는 책"
    List<ReadingLog> findByUserInAndReadDateGreaterThanEqualOrderByEndedAtDesc(List<User> users, LocalDate from);

    // 가장 최근 독서 기록 - 주간 경쟁 "읽고 있는 책"
    java.util.Optional<ReadingLog> findFirstByUserOrderByEndedAtDesc(User user);

    // 날짜별 독서 시간 합계 (서고 달력)
    @Query("select r.readDate, coalesce(sum(r.seconds), 0) from ReadingLog r where r.user = :user group by r.readDate order by r.readDate")
    List<Object[]> sumSecondsByUserGroupByDate(User user);

    @Query("select distinct r.readDate from ReadingLog r where r.user = :user order by r.readDate")
    List<LocalDate> findReadDatesByUser(User user);

    // 독서 기록이 하나라도 있는지
    boolean existsByUser(User user);

    // 이 책을 전에 읽은 기록이 있는지
    boolean existsByUserAndBookId(User user, Long bookId);

    // 이 책을 읽은 횟수 (타이머 기록 수)
    long countByUserAndBookId(User user, Long bookId);

    // 이 책을 특정 시각 뒤에 읽은 기록이 있는지 (완독 뒤 다시 읽기)
    boolean existsByUserAndBookIdAndEndedAtAfter(User user, Long bookId, java.time.LocalDateTime after);

    // 기간 내 유저별 독서 시간 합계: [유저 id, 합계(초)]
    @Query("select r.user.id, coalesce(sum(r.seconds), 0) from ReadingLog r " +
           "where r.user in :users and r.readDate between :from and :to group by r.user.id")
    List<Object[]> sumSecondsByUsersBetween(List<User> users, LocalDate from, LocalDate to);
}
