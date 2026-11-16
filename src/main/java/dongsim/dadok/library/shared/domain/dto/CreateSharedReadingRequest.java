package dongsim.dadok.library.shared.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

// 요청: { "joinID": 친구 아이디, "name": 친구 이름, "bookID" (없으면 title·author·total_page·isbn) }
// 기존 형식 { bookId, partnerEmail, partnerName } 도 받는다.
public record CreateSharedReadingRequest(
        @JsonAlias({"bookID", "book_id"}) Long bookId,
        @JsonAlias({"joinID", "join_id"}) String partnerUserId,
        String partnerEmail,
        @JsonAlias("name") String partnerName,
        String title,
        String author,
        @JsonProperty("total_page") @JsonAlias("totalPage") Integer totalPage,
        @JsonAlias("isbn13") String isbn
) {
}
