package dongsim.dadok.library.personal.repository;

import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.personal.domain.entity.PersonalReadingLog;

import dongsim.dadok.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PersonalReadingLogRepository extends JpaRepository<PersonalReadingLog, Long> {

    @Query("select distinct log.readDate from PersonalReadingLog log " +
            "where log.personalLibraryBook.user = :user order by log.readDate desc")
    List<LocalDate> findDistinctReadDatesByUser(User user);
}
