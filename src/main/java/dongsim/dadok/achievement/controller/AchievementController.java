package dongsim.dadok.achievement.controller;

import dongsim.dadok.achievement.domain.dto.AchievementResponse;
import dongsim.dadok.achievement.domain.dto.MessageResponse;
import dongsim.dadok.achievement.domain.dto.TitleRequest;
import dongsim.dadok.achievement.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    // 업적 목록 (마이페이지 업적 영역)
    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementResponse>> getAchievements(Authentication authentication) {
        return ResponseEntity.ok(achievementService.getAchievements(authentication.getName()));
    }

    // 칭호 사용
    @PutMapping("/title")
    public ResponseEntity<MessageResponse> equipTitle(Authentication authentication,
                                                      @Valid @RequestBody TitleRequest request) {
        return ResponseEntity.ok(achievementService.equipTitle(authentication.getName(), request.titleId()));
    }
}
