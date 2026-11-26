package dongsim.dadok.review.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dongsim.dadok.ai.domain.dto.AiMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiReviewSaveRequest(
        @JsonProperty("lib_book_id")
        @JsonAlias({"libBookId", "libBookID"})
        @NotNull(message = "서재 책 ID는 필수입니다.")
        Long libBookId,

        @JsonProperty("book_title")
        @JsonAlias("bookTitle")
        @NotBlank(message = "책 제목은 필수입니다.")
        @Size(max = 200, message = "책 제목은 200자 이내여야 합니다.")
        String bookTitle,

        @Size(max = 100, message = "저자명은 100자 이내여야 합니다.")
        String author,

        @Size(max = 20, message = "메시지는 최대 20개까지 보낼 수 있습니다.")
        List<@Valid AiMessage> messages
) {
    public AiReviewSaveRequest {
        messages = messages == null ? List.of() : messages;
    }
}
