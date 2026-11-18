package dongsim.dadok.note.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 작성: { bookID, book_page, date, note_content }
// 수정: { note_ID, book_page, date, note_content }
public record NoteRequest(
        @JsonAlias({"bookID", "book_id"}) Long bookId,
        @JsonProperty("note_ID") @JsonAlias({"note_id", "noteId"}) Long noteId,
        @JsonProperty("book_page") @JsonAlias("page") @Min(0) Integer bookPage,
        LocalDate date,
        @JsonProperty("note_content") @JsonAlias("content")
        @NotBlank(message = "인용구를 입력해주세요.")
        @Size(max = 200, message = "인용구는 200자까지 쓸 수 있습니다.") String content
) {
}
