package dongsim.dadok.library.personal.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

// 요청: { "bookId": 3 }  또는  { "title": "...", "author": "...", "total_page": 300 }
// bookId가 없으면 제목·저자로 책을 찾고, 없으면 새로 등록한다.
public record AddLibraryBookRequest(
        @JsonAlias("bookID") Long bookId,
        String title,
        String author,
        @JsonProperty("total_page") @JsonAlias("totalPage") Integer totalPage
) {
}
