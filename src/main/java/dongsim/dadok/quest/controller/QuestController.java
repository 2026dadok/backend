package dongsim.dadok.quest.controller;

import dongsim.dadok.quest.domain.dto.AttendanceResponse;
import dongsim.dadok.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // 출석: 로그인 토큰의 이메일로 사용자 확인
    @PostMapping("/attendance")
    public ResponseEntity<AttendanceResponse> attend(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(questService.attend(email));
    }
}
