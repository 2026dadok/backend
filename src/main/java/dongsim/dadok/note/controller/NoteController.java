package dongsim.dadok.note.controller;

import dongsim.dadok.achievement.domain.entity.AchievementCondition;
import dongsim.dadok.achievement.service.AchievementService;
import dongsim.dadok.auth.domain.entity.User;
import dongsim.dadok.auth.domain.repository.UserRepository;
import dongsim.dadok.book.domain.entity.Book;
import dongsim.dadok.book.repository.BookRepository;
import dongsim.dadok.note.domain.dto.NoteRequest;
import dongsim.dadok.note.domain.entity.Note;
import dongsim.dadok.note.repository.NoteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 인용구 노트 API (프론트 명세: /bookstore/note_page, /bookstore/notes)
@RestController
@RequestMapping("/bookstore")
@RequiredArgsConstructor
public class NoteController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final AchievementService achievementService;

    // 내 인용구 목록 (최신순)
    @GetMapping("/note_page")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> list(@AuthenticationPrincipal String userId) {
        User user = findUser(userId);
        List<Map<String, Object>> notes = noteRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toItem)
                .toList();
        return ResponseEntity.ok(Map.of("notes", notes));
    }

    // 작성 → { note_id, new_achievements }
    @PostMapping("/notes")
    @Transactional
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody NoteRequest request
    ) {
        User user = findUser(userId);
        if (request.bookId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "책 번호가 필요합니다.");
        }
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        Note saved = noteRepository.save(new Note(user, book, pageOf(request), dateOf(request), request.content().trim()));
        List<String> newAchievements = achievementService.check(
                user, AchievementCondition.QUOTE_COUNT, (int) noteRepository.countByUser(user));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("note_id", saved.getNoteId());
        body.put("new_achievements", newAchievements);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // 수정 (본인 것만)
    @PutMapping("/notes")
    @Transactional
    public ResponseEntity<Map<String, String>> update(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody NoteRequest request
    ) {
        Note note = findMyNote(findUser(userId), request.noteId());
        note.update(pageOf(request), dateOf(request), request.content().trim());
        return ResponseEntity.ok(Map.of("message", "인용구가 수정되었습니다."));
    }

    // 삭제 (본인 것만). 번호는 주소(note_ID) 또는 본문으로 받는다
    @DeleteMapping("/notes")
    @Transactional
    public ResponseEntity<Map<String, String>> delete(
            @AuthenticationPrincipal String userId,
            @RequestParam(value = "note_ID", required = false) Long noteIdParam,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Long noteId = noteIdParam;
        if (noteId == null && body != null && body.get("note_ID") instanceof Number n) {
            noteId = n.longValue();
        }
        Note note = findMyNote(findUser(userId), noteId);
        noteRepository.delete(note);
        return ResponseEntity.ok(Map.of("message", "인용구가 삭제되었습니다."));
    }

    private Map<String, Object> toItem(Note n) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("note_id", n.getNoteId());
        item.put("book_id", n.getBook().getBookId());
        item.put("title", n.getBook().getBookTitle());
        item.put("author", n.getBook().getAuthor());
        item.put("book_page", n.getBookPage());
        item.put("date", n.getNoteDate().toString());
        item.put("note_content", n.getContent());
        item.put("created_at", n.getCreatedAt().toString());
        return item;
    }

    private int pageOf(NoteRequest request) {
        return request.bookPage() == null ? 0 : request.bookPage();
    }

    private LocalDate dateOf(NoteRequest request) {
        return request.date() == null ? LocalDate.now(KST) : request.date();
    }

    private Note findMyNote(User user, Long noteId) {
        if (noteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인용구 번호가 필요합니다.");
        }
        return noteRepository.findByNoteIdAndUser(noteId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인용구를 찾을 수 없습니다."));
    }

    private User findUser(String userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
    }
}
