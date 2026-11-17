package dongsim.dadok.quest.controller;

import dongsim.dadok.quest.domain.dto.AttendanceResponse;
import dongsim.dadok.quest.domain.dto.DailyQuestListResponse;
import dongsim.dadok.quest.domain.dto.WeeklyQuestSummaryResponse;
import dongsim.dadok.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // 출석: 로그인 토큰의 아이디로 사용자 확인
    @PostMapping("/attendance")
    public ResponseEntity<AttendanceResponse> attend(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(questService.attend(userId));
    }

    // 일일 퀘스트 목록 + 완료 여부
    @GetMapping("/daily")
    public ResponseEntity<DailyQuestListResponse> daily(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(questService.getDailyQuestList(userId));
    }

    // 주간 퀘스트 + 진행도
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyQuestSummaryResponse> weekly(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(questService.getWeeklyQuestSummary(userId));
    }
}
