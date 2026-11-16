package dongsim.dadok.library.personal.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 요청: { "bookID": 3, "read_pages": 120 }
public record UpdateReadPagesRequest(
        @JsonAlias({"bookID", "book_id"}) @NotNull Long bookId,
        @JsonProperty("read_pages") @JsonAlias({"readPages", "currentPage"}) @NotNull @Min(0) Integer readPages
) {
}
