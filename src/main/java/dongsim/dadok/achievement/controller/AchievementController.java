package dongsim.dadok.achievement.controller;

import dongsim.dadok.achievement.domain.dto.AchievementResponse;
import dongsim.dadok.achievement.domain.dto.MessageResponse;
import dongsim.dadok.achievement.domain.dto.TitleRequest;
import dongsim.dadok.achievement.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    // 업적 목록 (마이페이지 업적 영역)
    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementResponse>> getAchievements(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(achievementService.getAchievements(email));
    }

    // 칭호 사용
    @PutMapping("/title")
    public ResponseEntity<MessageResponse> equipTitle(@AuthenticationPrincipal String email,
                                                      @Valid @RequestBody TitleRequest request) {
        return ResponseEntity.ok(achievementService.equipTitle(email, request.titleId()));
    }
}
