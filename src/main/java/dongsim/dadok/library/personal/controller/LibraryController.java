package dongsim.dadok.library.personal.controller;

import dongsim.dadok.library.personal.domain.dto.*;
import dongsim.dadok.library.personal.service.LibraryService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping("/books")
    public ResponseEntity<LibraryBookResponse> addBook(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddLibraryBookRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.addBook(userId, request));
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Map<String, String>> deleteBook(
            @AuthenticationPrincipal String userId,
            @PathVariable Long bookId
    ) {
        libraryService.deleteBook(userId, bookId);
        return ResponseEntity.ok(Map.of("message", "서재에서 삭제되었습니다."));
    }

    @GetMapping({"/my-page", "/my_page"})
    public ResponseEntity<LibraryPageResponse> getMyLibrary(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(libraryService.getMyLibrary(userId));
    }

    // 프론트 형식: PUT /library/pages { bookID, read_pages }
    @PutMapping("/pages")
    public ResponseEntity<LibraryBookResponse> updateReadPages(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateReadPagesRequest request
    ) {
        return ResponseEntity.ok(libraryService.updateProgress(
                userId, request.bookId(), new UpdatePersonalProgressRequest(request.readPages())));
    }

    @PutMapping("/books/{bookId}/progress")
    public ResponseEntity<LibraryBookResponse> updateProgress(
            @AuthenticationPrincipal String userId,
            @PathVariable Long bookId,
            @Valid @RequestBody UpdatePersonalProgressRequest request
    ) {
        return ResponseEntity.ok(libraryService.updateProgress(userId, bookId, request));
    }
}
