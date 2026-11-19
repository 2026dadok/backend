package dongsim.dadok.home.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.home.domain.dto.MainPageResponse;
import dongsim.dadok.quest.domain.dto.DailyQuestResponse;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MainPageService {

    private static final Logger LOG = LoggerFactory.getLogger(MainPageService.class);

    private final UserStatRepository userStatRepository;
    private final QuestService questService;

    public MainPageResponse getMainPage(String userId) {
        // 빠진 독서 시간 퀘스트를 먼저 채운다. 실패해도 메인 페이지는 보여준다
        try {
            questService.syncReadTimeQuests(userId);
        } catch (RuntimeException e) {
            LOG.warn("독서 시간 퀘스트 재확인 실패 userId={}", userId, e);
        }
        User user = userStatRepository.findUserByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        DailyQuestResponse allClear = questService.getAllClearQuest(user);
        return new MainPageResponse(
                questService.getXp(user),
                List.of(),                          // TODO: 나의 서재 연결
                List.of(),                          // TODO: 공동 서재 연결
                questService.getDailyQuests(user),
                allClear == null ? 0 : allClear.rewardXp(),
                allClear != null && allClear.completed(),
                questService.getWeeklyQuest(user),
                List.of(),                          // TODO: 이달의 추천 도서 연결
                null                                // TODO: 오늘의 문장(인용구) 연결
        );
    }
}
