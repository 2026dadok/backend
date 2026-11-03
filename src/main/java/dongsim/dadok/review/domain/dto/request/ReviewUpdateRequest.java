package dongsim.dadok.review.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {

    @NotBlank(message = "감상문 내용을 작성해주세요.")
    @Size(max = 300, message = "감상문은 최대 300자까지 작성할 수 있습니다.")
    private String review;
}