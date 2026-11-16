package dongsim.dadok.review.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 요청: { "bookID": 3, "feeling_content": "..." }
public record FeelingCreateRequest(
        @JsonAlias({"bookID", "book_id"}) @NotNull Long bookId,
        @JsonProperty("feeling_content") @JsonAlias("content")
        @NotBlank(message = "감상문 내용을 작성해주세요.")
        @Size(max = 300, message = "감상문은 최대 300자까지 작성할 수 있습니다.") String content
) {
}
