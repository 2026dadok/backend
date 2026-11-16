package dongsim.dadok.library.personal.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record LibraryBookResponse(
        Long libraryBookId,
        Long bookId,
        String bookTitle,
        String coverUrl,
        String author,
        int currentPage,
        int totalPages,
        int percent,
        boolean completed,
        LocalDateTime completedAt
) {
    // 프론트 호환 키
    @JsonProperty("book_id")
    public Long book_id() {
        return bookId;
    }
}
