package dongsim.dadok.quest.controller;

import dongsim.dadok.quest.domain.dto.AttendanceResponse;
import dongsim.dadok.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // 출석: 로그인한 사람의 이메일을 토큰(Authentication)에서 꺼냄
    @PostMapping("/attendance")
    public ResponseEntity<AttendanceResponse> attend(Authentication authentication) {
        return ResponseEntity.ok(questService.attend(authentication.getName()));
    }
}
