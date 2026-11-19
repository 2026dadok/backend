package dongsim.dadok.home.controller;

import dongsim.dadok.home.domain.dto.MainPageResponse;
import dongsim.dadok.home.service.MainPageService;
import dongsim.dadok.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MainPageController {

    private static final Logger LOG = LoggerFactory.getLogger(MainPageController.class);

    private final MainPageService mainPageService;
    private final QuestService questService;

    // 메인페이지 (로그인 후)
    @GetMapping("/auth/main_page")
    public ResponseEntity<MainPageResponse> getMainPage(@AuthenticationPrincipal String userId) {
        // 빠진 독서 시간 퀘스트를 먼저 채운다 (메인 페이지 조회 트랜잭션과 따로, DB 연결을 동시에 2개 잡지 않게)
        // 실패해도 메인 페이지는 보여준다
        try {
            questService.syncReadTimeQuests(userId);
        } catch (RuntimeException e) {
            LOG.warn("독서 시간 퀘스트 재확인 실패 userId={}", userId, e);
        }
        return ResponseEntity.ok(mainPageService.getMainPage(userId));
    }
}
