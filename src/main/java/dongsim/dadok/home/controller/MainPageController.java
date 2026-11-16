package dongsim.dadok.home.controller;

import dongsim.dadok.home.domain.dto.MainPageResponse;
import dongsim.dadok.home.service.MainPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MainPageController {

    private final MainPageService mainPageService;

    // 메인페이지 (로그인 후)
    @GetMapping("/auth/main_page")
    public ResponseEntity<MainPageResponse> getMainPage(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(mainPageService.getMainPage(userId));
    }
}
