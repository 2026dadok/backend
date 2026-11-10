package dongsim.dadok.review.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {
    @NotNull(message = "서재 책 ID는 필수입니다.")
    private Long libBookId;

    @NotBlank(message = "감상문 내용을 작성해주세요.")
    @Size(max = 300, message = "감상문은 최대 300자까지 작성할 수 있습니다.")
    private String review;

    private boolean isCompleted;

    private LocalDateTime reviewDate;
}