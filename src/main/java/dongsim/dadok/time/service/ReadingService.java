package dongsim.dadok.time.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.dto.ReadTimeRequest;
import dongsim.dadok.time.domain.dto.ReadTimeResponse;
import dongsim.dadok.time.domain.dto.TimerPageResponse;
import dongsim.dadok.time.domain.entity.ReadingLog;
import dongsim.dadok.time.domain.event.ReadingCompletedEvent;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long MAX_SECONDS_PER_DAY = 24 * 60 * 60;

    private final ReadingLogRepository readingLogRepository;
    private final UserStatRepository userStatRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 독서 시간 저장 (POST /times)
    public ReadTimeResponse saveReadTime(String userId, ReadTimeRequest request) {
        User user = findUser(userId);

        LocalDateTime now = LocalDateTime.now(KST);
        LocalDate today = now.toLocalDate();

        // 하루 합계 24시간 넘으면 거부 (시간 조작 방지)
        long before = readingLogRepository.sumSecondsByUserAndDate(user, today);
        if (before + request.seconds() > MAX_SECONDS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "하루 독서 시간은 24시간을 넘을 수 없습니다.");
        }

        // 저장 전 상태 (업적 판정용)
        boolean firstRead = !readingLogRepository.existsByUser(user);
        boolean readBefore = readingLogRepository.existsByUserAndBookId(user, request.bookId());

        // 1. 기록 저장
        readingLogRepository.save(new ReadingLog(user, request.bookId(), today, request.seconds(), now));

        // 2. 이벤트 발행 (퀘스트, 업적, 리그 처리)
        long todaySeconds = before + request.seconds();
        eventPublisher.publishEvent(new ReadingCompletedEvent(
                user, todaySeconds, today, firstRead, readBefore,
                now.minusSeconds(request.seconds()), now
        ));

        return new ReadTimeResponse("독서 시간이 저장되었습니다.", todaySeconds);
    }

    // 타이머 페이지 (GET /times/timer_page)
    @Transactional(readOnly = true)
    public TimerPageResponse getTimerPage(String userId) {
        User user = findUser(userId);
        long goalMinutes = user.getDefaultTimerTime() / 60L;   // DB는 초 단위
        long todaySeconds = readingLogRepository.sumSecondsByUserAndDate(user, LocalDate.now(KST));
        return new TimerPageResponse(goalMinutes, todaySeconds);
    }

    private User findUser(String userId) {
        return userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}

