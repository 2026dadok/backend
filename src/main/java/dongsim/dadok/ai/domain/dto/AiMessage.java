package dongsim.dadok.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AiMessage(
        @NotBlank(message = "메시지 역할은 필수입니다.")
        @Pattern(regexp = "user|assistant", message = "메시지 역할은 user 또는 assistant여야 합니다.")
        String role,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {
}
