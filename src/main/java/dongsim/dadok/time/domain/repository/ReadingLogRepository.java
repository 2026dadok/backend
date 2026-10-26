package dongsim.dadok.time.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.time.domain.entity.ReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {

    // 특정 날짜에 읽은 시간 합계 (초). 기록이 없으면 0
    @Query("select coalesce(sum(r.seconds), 0) from ReadingLog r where r.user = :user and r.readDate = :date")
    long sumSecondsByUserAndDate(User user, LocalDate date);
}
