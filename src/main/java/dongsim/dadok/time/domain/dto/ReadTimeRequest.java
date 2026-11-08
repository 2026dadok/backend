package dongsim.dadok.time.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 요청: { "bookId": 3, "readTime": 1801 }
public record ReadTimeRequest(
        @NotNull Long bookId,
        @NotNull @Min(1) @Max(21600) Integer readTime   // 초 단위, 1초 ~ 6시간
) {}
