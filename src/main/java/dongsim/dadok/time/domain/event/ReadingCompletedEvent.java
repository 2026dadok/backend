package dongsim.dadok.time.domain.event;

import dongsim.dadok.auth.domain.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ReadingCompletedEvent {
    private final User user;
    private final long todaySeconds;
    private final LocalDate today;
    private final boolean firstRead;
    private final boolean readBefore;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
}
