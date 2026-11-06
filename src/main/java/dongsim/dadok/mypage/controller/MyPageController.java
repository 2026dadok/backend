package dongsim.dadok.mypage.controller;

import dongsim.dadok.achievement.domain.dto.MessageResponse;
import dongsim.dadok.mypage.domain.dto.EditInfoRequest;
import dongsim.dadok.mypage.domain.dto.EditPageResponse;
import dongsim.dadok.mypage.domain.dto.MyPageResponse;
import dongsim.dadok.mypage.service.MyPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    // 마이페이지 통계
    @GetMapping("/users/my_page")
    public ResponseEntity<MyPageResponse> getMyPage(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(myPageService.getMyPage(email));
    }

    // 정보 수정 페이지
    @GetMapping("/users/edit_page")
    public ResponseEntity<EditPageResponse> getEditPage(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(myPageService.getEditPage(email));
    }

    // 정보 수정
    @PutMapping("/users/edit_info")
    public ResponseEntity<MessageResponse> editInfo(@AuthenticationPrincipal String email,
                                                    @Valid @RequestBody EditInfoRequest request) {
        return ResponseEntity.ok(myPageService.editInfo(email, request));
    }
}
