package dongsim.dadok.library.shared.controller;

import dongsim.dadok.library.shared.domain.dto.SharedReadingResponse;
import dongsim.dadok.library.shared.service.SharedReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 프론트 형식의 공동서재 상세: GET /library/joints/{id} (= GET /shared-readings/{id})
@RestController
@RequiredArgsConstructor
public class JointController {

    private final SharedReadingService sharedReadingService;

    @GetMapping("/library/joints/{sharedReadingId}")
    public ResponseEntity<SharedReadingResponse> getDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable Long sharedReadingId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return ResponseEntity.ok(sharedReadingService.getDetail(userId, sharedReadingId));
    }
}
