package dongsim.dadok.review.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dongsim.dadok.ai.domain.dto.AiMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
        String bookTitle,

        String author,

        List<@Valid AiMessage> messages
) {
    public AiReviewSaveRequest {
        messages = messages == null ? List.of() : messages;
    }
}
