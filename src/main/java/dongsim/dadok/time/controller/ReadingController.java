package dongsim.dadok.time.controller;

import dongsim.dadok.time.domain.dto.ReadTimeRequest;
import dongsim.dadok.time.domain.dto.ReadTimeResponse;
import dongsim.dadok.time.service.ReadingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/times")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;

    // 독서 시간 저장: 타이머 [책 덮기] → [네] 누르면 호출됨
    @PostMapping
    public ResponseEntity<ReadTimeResponse> saveReadTime(Authentication authentication,
                                                         @Valid @RequestBody ReadTimeRequest request) {
        return ResponseEntity.ok(readingService.saveReadTime(authentication.getName(), request));
    }
}
