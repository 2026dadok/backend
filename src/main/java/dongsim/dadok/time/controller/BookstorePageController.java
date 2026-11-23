package dongsim.dadok.time.controller;

import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.quest.domain.repository.UserStatRepository;
import dongsim.dadok.time.domain.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 서고 달력: GET /bookstore/bookstore_page → { calendar: [{ date: "yyyy-MM-dd", seconds }] }
@RestController
@RequiredArgsConstructor
public class BookstorePageController {

    private final UserStatRepository userStatRepository;
    private final ReadingLogRepository readingLogRepository;

    @GetMapping("/bookstore/bookstore_page")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getBookstorePage(@AuthenticationPrincipal String userId) {
        User user = userStatRepository.findUserByUserId(userId == null ? "" : userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        List<Map<String, Object>> calendar = readingLogRepository.sumSecondsByUserGroupByDate(user).stream()
                .map(row -> {
                    Map<String, Object> day = new LinkedHashMap<>();
                    day.put("date", row[0].toString());
                    day.put("seconds", ((Number) row[1]).longValue());
                    return day;
                })
                .toList();
        return ResponseEntity.ok(Map.of("calendar", calendar));
    }
}
