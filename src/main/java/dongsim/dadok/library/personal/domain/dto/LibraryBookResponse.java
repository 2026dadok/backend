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
        LocalDateTime completedAt,
        @JsonProperty("created_at") LocalDateTime createdAt,   // 서재에 담은 시각
        @JsonProperty("gained_xp") int gainedXp                // 이번 요청으로 받은 퀘스트 XP
) {
    // 프론트 호환 키
    @JsonProperty("book_id")
    public Long book_id() {
        return bookId;
    }

    @JsonProperty("book_title")
    public String book_title() {
        return bookTitle;
    }

    @JsonProperty("cover_url")
    public String cover_url() {
        return coverUrl;
    }

    @JsonProperty("total_page")
    public int total_page() {
        return totalPages;
    }

    @JsonProperty("read_page")
    public int read_page() {
        return currentPage;
    }

    @JsonProperty("finished")
    public boolean finished() {
        return completed;
    }
}
