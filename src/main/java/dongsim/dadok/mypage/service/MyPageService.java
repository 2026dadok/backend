package dongsim.dadok.mypage.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.league.service.LeagueService;
import dongsim.dadok.profile.service.ProfileImageService;
import dongsim.dadok.mypage.domain.dto.EditPageResponse;
import dongsim.dadok.mypage.domain.dto.MyPageResponse;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserStatRepository userStatRepository;
    private final ReadingLogRepository readingLogRepository;
    private final LeagueService leagueService;
    private final ProfileImageService profileImageService;

    public MyPageResponse getMyPage(String email) {
        User user = userStatRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        long allReadingTime = readingLogRepository.sumSecondsByUser(user);
        int sequenceDay = maxStreak(readingLogRepository.findReadDatesByUser(user));

        return new MyPageResponse(
                allReadingTime,
                sequenceDay,
                0,      // TODO: 쪽수 기록 연결
                leagueService.countTop5(user),
                profileImageService.profileUrl(user),
                profileImageService.bannerUrl(user)
        );
    }

    // 정보 수정 페이지 (비밀번호 제외)
    public EditPageResponse getEditPage(String email) {
        User user = userStatRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        return new EditPageResponse(
                user.getName(),
                user.getBirthDate(),
                user.getDefaultTimerTime(),
                user.getEmail()
        );
    }

    // 날짜 목록(오래된 순)에서 가장 길게 이어진 연속 일수
    private int maxStreak(List<LocalDate> dates) {
        int max = 0;
        int current = 0;
        LocalDate prev = null;
        for (LocalDate date : dates) {
            current = (prev != null && prev.plusDays(1).equals(date)) ? current + 1 : 1;
            max = Math.max(max, current);
            prev = date;
        }
        return max;
    }
}
