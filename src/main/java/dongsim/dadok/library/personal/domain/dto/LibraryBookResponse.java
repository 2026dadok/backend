package dongsim.dadok.library.personal.domain.dto;

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
}
