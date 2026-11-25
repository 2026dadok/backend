package dongsim.dadok.ai.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiInterviewRequest(
        @JsonProperty("book_title")
        @NotBlank(message = "책 제목은 필수입니다.")
        String bookTitle,

        String author,

        List<@Valid AiMessage> messages
) {
    public AiInterviewRequest {
        messages = messages == null ? List.of() : messages;
    }
}
