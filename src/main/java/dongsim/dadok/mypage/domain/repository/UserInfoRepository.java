package dongsim.dadok.mypage.domain.repository;

import dongsim.dadok.auth.domain.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;

public interface UserInfoRepository extends Repository<User, Long> {

    // 이름, 생년월일, 목표 시간, 비밀번호 수정 (이메일은 수정 불가)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update User u
               set u.name = :name,
                   u.birthDate = :birthDate,
                   u.defaultTimerTime = :goalTime,
                   u.password = :password
             where u.id = :id
            """)
    int updateInfo(Long id, String name, LocalDate birthDate, Integer goalTime, String password);
}
