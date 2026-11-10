package dongsim.dadok.library.shared.domain.dto;

import dongsim.dadok.library.shared.domain.entity.SharedReadingStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReflectionRequest(@NotBlank @Size(max = 300) String content) {
}
