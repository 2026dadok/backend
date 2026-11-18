package dongsim.dadok.library.personal.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

// 요청: { "bookId": 3 }  또는  { "title", "author", "total_page", "isbn", "cover_url" }
// bookId가 없으면 ISBN → 제목·저자 순으로 책을 찾고, 없으면 새로 등록한다.
public record AddLibraryBookRequest(
        @JsonAlias("bookID") Long bookId,
        String title,
        String author,
        @JsonProperty("total_page") @JsonAlias("totalPage") @Min(0) @Max(20000) Integer totalPage,
        @JsonAlias("isbn13") String isbn,
        @JsonProperty("cover_url") @JsonAlias("coverUrl") String coverUrl
) {
}
