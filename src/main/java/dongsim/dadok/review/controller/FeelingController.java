package dongsim.dadok.review.controller;

import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.auth.domain.repository.UserRepository;
import dongsim.dadok.library.personal.domain.entity.PersonalLibraryBook;
import dongsim.dadok.library.personal.repository.PersonalLibraryBookRepository;
import dongsim.dadok.review.domain.dto.request.FeelingCreateRequest;
import dongsim.dadok.review.domain.entity.Review;
import dongsim.dadok.review.repository.ReviewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 프론트 형식의 감상문 API (책 번호 기준). 저장은 book_review_manage(서재 책 기준)에 한다.
//   POST /bookstore/feelings           { bookID, feeling_content } → { feeling_id, new_achievements }
//   GET  /bookstore/feeling_page?bookID= → { feelings: [{ feeling_id, day, feeling_content, date }] }
@RestController
@RequestMapping("/bookstore")
@RequiredArgsConstructor
public class FeelingController {

    private final UserRepository userRepository;
    private final PersonalLibraryBookRepository personalLibraryBookRepository;
    private final AchievementService achievementService;
    private final ReviewRepository reviewRepository;

    @PostMapping("/feelings")
    @Transactional
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody FeelingCreateRequest request
    ) {
        PersonalLibraryBook libraryBook = findLibraryBook(userId, request.bookId());
        Review saved = reviewRepository.save(Review.builder()
                .libBookId(libraryBook.getPersonalLibraryBookId())
                .writer(libraryBook.getUser())
                .review(request.content().trim())
                .build());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("feeling_id", saved.getId());
        // 감상문 작성 개수 업적 (개인 서재 + 공동서재 감상문)
        body.put("new_achievements", achievementService.checkFeelingCount(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/feeling_page")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> list(
            @AuthenticationPrincipal String userId,
            @RequestParam("bookID") Long bookId
    ) {
        PersonalLibraryBook libraryBook = findLibraryBook(userId, bookId);
        List<Review> reviews = reviewRepository.findAllByLibBookIdOrderByReviewDateAsc(libraryBook.getPersonalLibraryBookId());

        List<Map<String, Object>> feelings = new ArrayList<>();
        for (int i = 0; i < reviews.size(); i++) {
            Review r = reviews.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("feeling_id", r.getId());
            item.put("day", i + 1);
            item.put("feeling_content", r.getReview());
            item.put("date", r.getReviewDate().toString());
            feelings.add(item);
        }
        return ResponseEntity.ok(Map.of("feelings", feelings));
    }

    private PersonalLibraryBook findLibraryBook(String userId, Long bookId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
        return personalLibraryBookRepository.findByUserAndBookBookId(user, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "개인 서재에 추가되지 않은 책입니다."));
    }
}
