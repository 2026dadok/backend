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
    @Query("select distinct r.readDate from ReadingLog r where r.user = :user order by r.readDate")
    List<LocalDate> findReadDatesByUser(User user);
}
