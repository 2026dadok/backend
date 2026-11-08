package dongsim.dadok.library.shared.domain.dto;

import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SharedReadingResponse(
        Long sharedReadingId,
        Long bookId,
        String bookTitle,
        String coverUrl,
        SharedReadingStatus status,
        String winnerUserId,
        List<MemberProgressResponse> members
) {
    public record MemberProgressResponse(String userId, String name, int currentPage, int totalPages, int progressPercent, LocalDateTime completedAt) {
    }
}
