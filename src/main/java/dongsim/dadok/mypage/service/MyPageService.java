package dongsim.dadok.mypage.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.league.service.LeagueService;
import dongsim.dadok.profile.service.ProfileImageService;
import dongsim.dadok.achievement.domain.dto.MessageResponse;
import dongsim.dadok.auth.domain.repository.RefreshTokenRepository;
import dongsim.dadok.mypage.domain.dto.EditInfoRequest;
import dongsim.dadok.mypage.domain.dto.EditPageResponse;
import dongsim.dadok.mypage.domain.dto.MyPageResponse;
import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.mypage.domain.repository.UserInfoRepository;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserInfoRepository userInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AchievementService achievementService;

    @Transactional
    public MyPageResponse getMyPage(String userId) {
        User user = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        long allReadingMinutes = readingLogRepository.sumSecondsByUser(user) / 60;
        int sequenceDay = maxStreak(readingLogRepository.findReadDatesByUser(user));
        List<MyPageResponse.Achievement> achievements = achievementService.getAchievements(userId).stream()
                .map(a -> new MyPageResponse.Achievement(
                        a.achievementId(), a.title(), a.description(), a.achieved(), a.equipped()))
                .toList();

        return new MyPageResponse(
                user.getUserId(),
                user.getName(),
                achievementService.getEquippedTitle(user),
                allReadingMinutes,
                sequenceDay,
                0,      // TODO: 쪽수 기록 연결
                leagueService.countTop5(user),
                profileImageService.profileUrl(user),
                profileImageService.bannerUrl(user),
                achievements
        );
    }

    // 정보 수정 페이지 (비밀번호 제외)
    public EditPageResponse getEditPage(String userId) {
        User user = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        return new EditPageResponse(
                user.getUserId(),
                user.getName(),
                user.getBirthDate(),
                toHourMinute(user.getDefaultTimerTime()),
                user.getEmail(),
                profileImageService.profileUrl(user),
                profileImageService.bannerUrl(user)
        );
    }

    // 정보 수정 (이메일 제외, 새 비밀번호가 있을 때만 비밀번호 변경)
    @Transactional
    public MessageResponse editInfo(String userId, EditInfoRequest request) {
        User user = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        String password = user.getPassword();
        if (hasText(request.newPassword())) {
            if (!hasText(request.currentPassword())
                    || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
            }
            password = passwordEncoder.encode(request.newPassword());
        }

        userInfoRepository.updateInfo(
                user.getId(),
                request.name().trim(),
                request.birthdate(),
                request.goalSeconds(),
                password
        );
        return new MessageResponse("정보가 수정되었습니다.");
    }

    // 회원탈퇴 (이메일·비밀번호만 삭제, 다른 기록은 유지)
    @Transactional
    public MessageResponse withdraw(String userId) {
        User user = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        refreshTokenRepository.deleteByUserId(userId);
        // email은 NOT NULL·UNIQUE라서 비우는 대신 사용자별 고유값으로 바꿈 (같은 이메일로 재가입 가능)
        userInfoRepository.withdraw(user.getId(), "withdrawn_" + user.getId());
        return new MessageResponse("회원탈퇴가 완료되었습니다.");
    }

    // 초 → "HH:mm"
    private String toHourMinute(Integer seconds) {
        int s = seconds == null ? 0 : seconds;
        return String.format("%02d:%02d", s / 3600, (s % 3600) / 60);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
