package dongsim.dadok.achievement.service;

import dongsim.dadok.achievement.domain.dto.AchievementResponse;
import dongsim.dadok.achievement.domain.dto.MessageResponse;
import dongsim.dadok.achievement.domain.entity.Achievement;
import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.domain.entity.UserAchievement;
import dongsim.dadok.achievement.domain.repository.AchievementRepository;
import dongsim.dadok.achievement.domain.repository.UserAchievementRepository;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.entity.Tier;
import dongsim.dadok.quest.domain.entity.UserStat;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AchievementService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserStatRepository userStatRepository;

    // 숫자 조건 업적 검사: value가 목표값 이상인 업적을 모두 달성 처리
    // 예) check(user, FINISH_COUNT, 10) → 완독의 맛(1), 똑똑이(10) 달성
    // 반환값: 새로 달성한 칭호명 목록
    public List<String> check(User user, AchievementCondition condition, int value) {
        List<String> newly = new ArrayList<>();
        for (Achievement a : achievementRepository.findAllByConditionType(condition)) {
            int target = a.getTargetValue() == null ? 1 : a.getTargetValue();
            if (value >= target && grant(user, a)) {
                newly.add(a.getTitle());
            }
        }
        return newly;
    }

    // 순위 조건 업적 검사: rank가 목표 순위 이내인 업적을 달성 처리
    // 예) checkRank(user, WEEKLY_RANK_ENTER, 1) → 독서왕(10위), 일일천하(1위) 달성
    public List<String> checkRank(User user, AchievementCondition condition, int rank) {
        List<String> newly = new ArrayList<>();
        for (Achievement a : achievementRepository.findAllByConditionType(condition)) {
            if (a.getTargetValue() != null && rank <= a.getTargetValue() && grant(user, a)) {
                newly.add(a.getTitle());
            }
        }
        return newly;
    }

    // 티어 업적 검사: 현재 티어까지의 업적을 모두 달성 처리
    public List<String> checkTier(User user, int xp) {
        return check(user, AchievementCondition.TIER_REACH, Tier.of(xp).level());
    }

    // 업적 목록 + 달성 여부 + 사용 여부
    public List<AchievementResponse> getAchievements(String email) {
        User user = findUser(email);
        UserStat stat = getOrCreateStat(user);
        checkTier(user, stat.getXp());

        Set<Long> achievedIds = userAchievementRepository.findAchievementIdsByUser(user);
        Long equippedId = stat.getEquippedAchievementId();

        return achievementRepository.findAllByOrderByIdAsc().stream()
                .map(a -> new AchievementResponse(
                        a.getId(),
                        a.getTitle(),
                        a.getDescription(),
                        achievedIds.contains(a.getId()),
                        a.getId().equals(equippedId)))
                .toList();
    }

    // 칭호 사용 (PUT /users/title)
    public MessageResponse equipTitle(String email, Long achievementId) {
        User user = findUser(email);
        UserStat stat = getOrCreateStat(user);
        checkTier(user, stat.getXp());

        if (!achievementRepository.existsById(achievementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 칭호입니다.");
        }
        if (!userAchievementRepository.existsByUserAndAchievement_Id(user, achievementId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "달성하지 않은 칭호입니다.");
        }

        stat.equipTitle(achievementId);
        return new MessageResponse("칭호가 변경되었습니다.");
    }

    // 사용 중인 칭호명 (없으면 null)
    @Transactional(readOnly = true)
    public String getEquippedTitle(User user) {
        return userStatRepository.findByUser(user)
                .map(UserStat::getEquippedAchievementId)
                .flatMap(achievementRepository::findById)
                .map(Achievement::getTitle)
                .orElse(null);
    }

    private boolean grant(User user, Achievement achievement) {
        if (userAchievementRepository.existsByUserAndAchievement(user, achievement)) {
            return false;
        }
        userAchievementRepository.save(new UserAchievement(user, achievement, LocalDateTime.now(KST)));
        return true;
    }

    private UserStat getOrCreateStat(User user) {
        return userStatRepository.findByUser(user)
                .orElseGet(() -> userStatRepository.save(new UserStat(user)));
    }

    private User findUser(String email) {
        return userStatRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
