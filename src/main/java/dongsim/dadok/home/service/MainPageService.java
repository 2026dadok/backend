package dongsim.dadok.home.service;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.home.domain.dto.MainPageResponse;
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

    private final UserStatRepository userStatRepository;
    private final QuestService questService;

    public MainPageResponse getMainPage(String email) {
        User user = userStatRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        return new MainPageResponse(
                questService.getXp(user),
                List.of(),                          // TODO: 나의 서재 연결
                List.of(),                          // TODO: 공동 서재 연결
                questService.getDailyQuests(user),
                questService.getWeeklyQuest(user),
                List.of(),                          // TODO: 이달의 추천 도서 연결
                null                                // TODO: 오늘의 문장(인용구) 연결
        );
    }
}
